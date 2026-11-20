package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.service.ExcelImportResult.RowError;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 원본 Project_Upload.asp 대응 (DEXT.FileUpload + Jet OLEDB → MultipartFile + Apache POI, .xls/.xlsx 둘 다 가능)
 * <p>
 * 컬럼 순서 (1행은 헤더라 건너뜀) - 화면의 '양식 다운로드'({@link ExcelTemplateService})와 같은 순서:
 * 프로젝트코드 | 제목 | 종류 | 담당자 | 클라이언트 | 매출금액(부가세별도)
 * <ul>
 *   <li>월은 엑셀에 없고, 업로드할 때 화면에서 조회 중인 월을 받는다.</li>
 *   <li>직접비는 업로드 후 화면에서 입력한다.</li>
 *   <li>예전 8열 양식(... | 클라이언트 담당자 | 금액 | 월)도 헤더(F열 "클라이언트 담당자")를 보고 그대로 읽는다.
 *       이때 월은 엑셀의 '월' 열을 쓴다.</li>
 * </ul>
 * <p>
 * Kcode 열은 V5에서 삭제됐다. 예전 양식(A열 헤더가 "Kcode")을 올리면 열이 한 칸씩 밀려
 * 모든 행이 엉뚱한 사유로 실패하므로, 헤더를 보고 양식이 바뀌었다고 바로 안내한다.
 * No(프로젝트 번호)는 DB가 자동으로 매기므로 엑셀에서 받지 않는다.
 * <p>
 * 원본은 문제 있는 행을 그냥 건너뛰고 나머지만 넣어서, 몇 건이 빠졌는지 아무도 모르는 일이 생겼었음.
 * 그래서 <b>한 행이라도 문제가 있으면 전부 반영하지 않고</b> 행 번호 + 사유를 돌려주도록 변경.
 * (반쯤 들어간 상태에서 엑셀 고쳐서 다시 올리면 중복이 생기는 것도 막을 수 있음)
 */
@Service
public class ExcelProjectImportService {

    /**
     * 연도 2자리 - 종류 약어 2~3자리 - 클라이언트 약어 3자리 - 순번 2자리 (예: 26-DSH-BOT-01).
     * year_code 가 앞 2자리에서 파생되므로 형식이 중요 (DB에도 같은 CHECK 제약이 있음)
     */
    private static final Pattern PROJECT_CODE = Pattern.compile("^\\d{2}-[A-Z]{2,3}-[A-Z]{3}-\\d{2}$");
    /** "3", "03", "3월" 허용 */
    private static final Pattern MONTH = Pattern.compile("^(0?[1-9]|1[0-2])월?$");
    /** 양식 다운로드 헤더 (순서 = 열 순서) */
    public static final List<String> TEMPLATE_HEADERS = List.of("프로젝트코드", "제목", "종류", "담당자", "클라이언트", "매출금액");
    private static final String LEGACY_CLIENT_MANAGER_HEADER = "클라이언트 담당자";
    private static final int LEGACY_COLUMN_COUNT = 8;

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    public ExcelProjectImportService(ProjectRepository projectRepository, AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ExcelImportResult importFrom(MultipartFile file, String month) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return importFrom(in, month);
        }
    }

    /**
     * @param month 새 양식일 때 모든 행에 넣을 월 ("1"~"12" / "01"~"12"). 예전 8열 양식이면 무시하고 엑셀의 '월' 열을 쓴다.
     */
    @Transactional
    public ExcelImportResult importFrom(InputStream in, String month) throws IOException {
        List<RowError> errors = new ArrayList<>();
        Map<String, ParsedRow> rowsByCode = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row header = sheet.getRow(0);
            if (header != null && readString(header, 0, formatter).equalsIgnoreCase("Kcode")) {
                return ExcelImportResult.failed(List.of(new RowError(1, null,
                        "예전 양식입니다. Kcode 열이 삭제되었으니 A열(Kcode)을 지우고 다시 올려 주세요.")));
            }
            boolean legacy = header != null
                    && readString(header, 5, formatter).replace(" ", "").equals(LEGACY_CLIENT_MANAGER_HEADER.replace(" ", ""));
            String formMonth = legacy ? null : normalizeMonth(month == null ? "" : month);
            if (!legacy && formMonth == null) {
                return ExcelImportResult.failed(List.of(new RowError(1, null,
                        "등록할 월을 알 수 없습니다. 화면에서 조회 월을 선택한 뒤 다시 올려 주세요.")));
            }

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isBlankRow(row, formatter, legacy ? LEGACY_COLUMN_COUNT : TEMPLATE_HEADERS.size())) {
                    continue;
                }
                int excelRowNumber = rowIdx + 1; // 사용자에게는 엑셀에 보이는 행 번호로 안내
                ParsedRow parsed = legacy ? parseLegacyRow(row, formatter) : parseRow(row, formatter, formMonth);

                String problem = validate(parsed, rowsByCode);
                if (problem != null) {
                    errors.add(new RowError(excelRowNumber, parsed.projectCode, problem));
                    continue;
                }
                rowsByCode.put(parsed.projectCode, parsed.withRowNumber(excelRowNumber));
            }
        }

        // 이미 DB에 있는 코드는 한 번에 조회 (행마다 쿼리 날리지 않게)
        if (!rowsByCode.isEmpty()) {
            for (String existing : projectRepository.findExistingProjectCodes(rowsByCode.keySet())) {
                ParsedRow dup = rowsByCode.get(existing);
                errors.add(new RowError(dup.rowNumber, existing, "이미 등록된 프로젝트코드입니다."));
            }
        }

        if (!errors.isEmpty()) {
            errors.sort(Comparator.comparingInt(RowError::rowNumber));
            return ExcelImportResult.failed(errors);
        }

        List<Project> projects = rowsByCode.values().stream().map(ParsedRow::toProject).toList();
        projectRepository.saveAll(projects);
        for (Project p : projects) {
            auditLogService.record(p, AuditAction.EXCEL_IMPORT, null,
                    p.getTitle() + " / 매출 " + ProjectService.plain(p.getRevenueAmount()));
        }
        return ExcelImportResult.success(projects.size());
    }

    private String validate(ParsedRow row, Map<String, ParsedRow> alreadyParsed) {
        if (row.projectCode.isBlank()) {
            return "프로젝트코드가 비어 있습니다.";
        }
        if (!PROJECT_CODE.matcher(row.projectCode).matches()) {
            return "프로젝트코드 형식이 올바르지 않습니다. (예: 26-DSH-BOT-01)";
        }
        if (alreadyParsed.containsKey(row.projectCode)) {
            return "파일 안에 같은 프로젝트코드가 중복되어 있습니다. (" + alreadyParsed.get(row.projectCode).rowNumber + "행)";
        }
        if (row.title.isBlank()) {
            return "제목이 비어 있습니다.";
        }
        if (row.amount == null) {
            return "금액을 숫자로 읽을 수 없습니다: '" + row.rawAmount + "'";
        }
        if (row.amount.signum() < 0) {
            return "금액이 음수입니다.";
        }
        if (row.month == null) {
            return row.rawMonth.isBlank()
                    ? "월이 비어 있습니다."
                    : "월은 1~12 사이 숫자여야 합니다: '" + row.rawMonth + "'";
        }
        return null;
    }

    /** 현재 양식: 프로젝트코드 | 제목 | 종류 | 담당자 | 클라이언트 | 매출금액 */
    private ParsedRow parseRow(Row row, DataFormatter formatter, String month) {
        String rawAmount = readString(row, 5, formatter);
        return new ParsedRow(
                0,
                readString(row, 0, formatter).toUpperCase(Locale.ROOT),
                readString(row, 1, formatter),
                readString(row, 2, formatter),
                readString(row, 3, formatter),
                readString(row, 4, formatter),
                null,
                readAmount(row.getCell(5), rawAmount),
                rawAmount,
                month,
                month);
    }

    /** 예전 양식: 프로젝트코드 | 제목 | 종류 | 제작 담당자 | 클라이언트 | 클라이언트 담당자 | 금액 | 월 */
    private ParsedRow parseLegacyRow(Row row, DataFormatter formatter) {
        String rawAmount = readString(row, 6, formatter);
        String rawMonth = readString(row, 7, formatter);
        return new ParsedRow(
                0,
                readString(row, 0, formatter).toUpperCase(Locale.ROOT),
                readString(row, 1, formatter),
                readString(row, 2, formatter),
                readString(row, 3, formatter),
                readString(row, 4, formatter),
                readString(row, 5, formatter),
                readAmount(row.getCell(6), rawAmount),
                rawAmount,
                normalizeMonth(rawMonth),
                rawMonth);
    }

    /** "3" / "03" / "3월" → "03". 형식이 틀리면 null */
    private static String normalizeMonth(String raw) {
        String trimmed = raw.replace(" ", "");
        if (!MONTH.matcher(trimmed).matches()) {
            return null;
        }
        return String.format("%02d", Integer.parseInt(trimmed.replace("월", "")));
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, int columnCount) {
        for (int c = 0; c < columnCount; c++) {
            if (!readString(row, c, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readString(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    /** 숫자 셀은 그대로, 문자 셀은 "￦1,200,000" 같은 값도 허용. 못 읽으면 null (예전엔 0으로 들어갔음) */
    private BigDecimal readAmount(Cell cell, String raw) {
        if (cell == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String numeric = raw.replaceAll("[\\s,￦₩원]", "");
        try {
            return new BigDecimal(numeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record ParsedRow(int rowNumber, String projectCode, String title, String projectType,
                             String manager, String clientName, String clientManager,
                             BigDecimal amount, String rawAmount, String month, String rawMonth) {

        ParsedRow withRowNumber(int n) {
            return new ParsedRow(n, projectCode, title, projectType, manager, clientName, clientManager,
                    amount, rawAmount, month, rawMonth);
        }

        Project toProject() {
            Project p = Project.newProject();
            p.setProjectCode(projectCode);
            p.setTitle(title);
            p.setProjectType(projectType);
            p.setManager(manager);
            p.setClientName(clientName);
            p.setClientManager(clientManager);
            p.setRevenueAmount(amount);
            p.setMonthCode(month);
            return p;
        }
    }
}
