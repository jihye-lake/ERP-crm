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
 * 컬럼 순서 (1행은 헤더라 건너뜀):
 * Kcode | 프로젝트코드 | 제목 | 종류 | 제작 담당자 | 클라이언트 | 클라이언트 담당자 | 금액(부가세별도)
 * <p>
 * 원본은 문제 있는 행을 그냥 건너뛰고 나머지만 넣어서, 몇 건이 빠졌는지 아무도 모르는 일이 생겼었음.
 * 그래서 <b>한 행이라도 문제가 있으면 전부 반영하지 않고</b> 행 번호 + 사유를 돌려주도록 변경.
 * (반쯤 들어간 상태에서 엑셀 고쳐서 다시 올리면 중복이 생기는 것도 막을 수 있음)
 */
@Service
public class ExcelProjectImportService {

    /** P + 연도 2자리 + 월 2자리 + 순번 (예: P26050001). year_code/month_code 가 여기서 파생되므로 형식이 중요 */
    private static final Pattern PROJECT_CODE = Pattern.compile("^P\\d{2}(0[1-9]|1[0-2])\\d{2,15}$");
    private static final int COLUMN_COUNT = 8;

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    public ExcelProjectImportService(ProjectRepository projectRepository, AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ExcelImportResult importFrom(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return importFrom(in);
        }
    }

    @Transactional
    public ExcelImportResult importFrom(InputStream in) throws IOException {
        List<RowError> errors = new ArrayList<>();
        Map<String, ParsedRow> rowsByCode = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                int excelRowNumber = rowIdx + 1; // 사용자에게는 엑셀에 보이는 행 번호로 안내
                ParsedRow parsed = parseRow(row, formatter);

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
            return "프로젝트코드 형식이 올바르지 않습니다. (예: P26050001)";
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
        return null;
    }

    private ParsedRow parseRow(Row row, DataFormatter formatter) {
        String rawAmount = readString(row, 7, formatter);
        return new ParsedRow(
                0,
                readString(row, 0, formatter),
                readString(row, 1, formatter).toUpperCase(Locale.ROOT),
                readString(row, 2, formatter),
                readString(row, 3, formatter),
                readString(row, 4, formatter),
                readString(row, 5, formatter),
                readString(row, 6, formatter),
                readAmount(row.getCell(7), rawAmount),
                rawAmount);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int c = 0; c < COLUMN_COUNT; c++) {
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

    private record ParsedRow(int rowNumber, String kcode, String projectCode, String title, String projectType,
                             String manager, String clientName, String clientManager,
                             BigDecimal amount, String rawAmount) {

        ParsedRow withRowNumber(int n) {
            return new ParsedRow(n, kcode, projectCode, title, projectType, manager, clientName, clientManager, amount, rawAmount);
        }

        Project toProject() {
            Project p = Project.newProject();
            p.setKcode(kcode.isBlank() ? null : kcode);
            p.setProjectCode(projectCode);
            p.setTitle(title);
            p.setProjectType(projectType);
            p.setManager(manager);
            p.setClientName(clientName);
            p.setClientManager(clientManager);
            p.setRevenueAmount(amount);
            return p;
        }
    }
}
