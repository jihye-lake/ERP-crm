package com.nexora.erp.service;

import com.nexora.erp.domain.Client;
import com.nexora.erp.domain.ProjectType;
import com.nexora.erp.repository.ClientRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 프로젝트 일괄 업로드용 엑셀 양식(.xlsx).
 * <p>
 * 1번 시트 "프로젝트" : 헤더 한 줄만 있음 (예시 행을 넣으면 지우지 않고 올렸을 때 그대로 등록되므로 넣지 않음)
 * 2번 시트 "작성 안내" : 프로젝트코드 규칙 + 종류/클라이언트 약어 표
 * <p>
 * 열 순서는 {@link ExcelProjectImportService#TEMPLATE_HEADERS} 와 같아야 한다.
 * 직접비는 업로드 후 화면에서 입력하므로 양식에 없음.
 */
@Service
public class ExcelTemplateService {

    private static final int MAX_INPUT_ROW = 1000;

    private final ClientRepository clientRepository;

    public ExcelTemplateService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public byte[] projectUploadTemplate() throws IOException {
        List<Client> clients = clientRepository.findAll(Sort.by("name"));
        List<String> headers = ExcelProjectImportService.TEMPLATE_HEADERS;

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(wb);
            CellStyle text = wb.createCellStyle();
            text.setDataFormat(wb.createDataFormat().getFormat("@"));
            CellStyle amount = wb.createCellStyle();
            amount.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            // ---- 1. 입력 시트 ----
            Sheet sheet = wb.createSheet("프로젝트");
            Row header = sheet.createRow(0);
            int[] widths = {18, 40, 14, 12, 20, 16};
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(c, widths[c] * 256);
                // 프로젝트코드 등은 문자, 매출금액은 콤마 표시
                sheet.setDefaultColumnStyle(c, c == 5 ? amount : text);
            }
            sheet.createFreezePane(0, 1);

            DataValidationHelper dv = sheet.getDataValidationHelper();
            // 종류: 드롭다운
            String[] typeLabels = Arrays.stream(ProjectType.values()).map(ProjectType::getLabel).toArray(String[]::new);
            addValidation(sheet, dv.createValidation(dv.createExplicitListConstraint(typeLabels),
                    new CellRangeAddressList(1, MAX_INPUT_ROW, 2, 2)), "종류", "목록에서 선택해 주세요.");
            // 매출금액: 0 이상 숫자
            addValidation(sheet, dv.createValidation(
                    dv.createDecimalConstraint(DataValidationConstraint.OperatorType.GREATER_OR_EQUAL, "0", null),
                    new CellRangeAddressList(1, MAX_INPUT_ROW, 5, 5)), "매출금액", "0 이상의 숫자만 입력해 주세요.");

            // ---- 2. 작성 안내 시트 ----
            Sheet guide = wb.createSheet("작성 안내");
            guide.setColumnWidth(0, 22 * 256);
            guide.setColumnWidth(1, 60 * 256);
            int r = 0;
            r = line(guide, r, headerStyle, "항목", "설명");
            r = line(guide, r, null, "프로젝트코드", "연도2자리-종류약어-클라이언트약어-순번2자리 (예: 26-DSH-BOT-01)");
            r = line(guide, r, null, "제목", "필수");
            r = line(guide, r, null, "종류", "목록에서 선택 (아래 표 참고)");
            r = line(guide, r, null, "담당자", "제작 담당자 이름");
            r = line(guide, r, null, "클라이언트", "클라이언트 이름 (아래 표 참고)");
            r = line(guide, r, null, "매출금액", "부가세 별도, 숫자만 (콤마/￦ 있어도 됨)");
            r = line(guide, r, null, "월", "엑셀에는 없음. 업로드할 때 화면에서 조회 중인 월로 등록됩니다.");
            r = line(guide, r, null, "직접비", "업로드 후 화면에서 프로젝트별로 입력합니다.");
            r++;
            r = line(guide, r, headerStyle, "종류", "약어");
            for (ProjectType t : ProjectType.values()) {
                r = line(guide, r, null, t.getLabel(), t.getAbbr());
            }
            r++;
            r = line(guide, r, headerStyle, "클라이언트", "약어");
            for (Client c : clients) {
                r = line(guide, r, null, c.getName(), c.getAbbr());
            }

            wb.setActiveSheet(0);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void addValidation(Sheet sheet, DataValidation validation, String title, String message) {
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
    }

    private static int line(Sheet sheet, int rowIdx, CellStyle style, String a, String b) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        c0.setCellValue(a);
        Cell c1 = row.createCell(1);
        c1.setCellValue(b);
        if (style != null) {
            c0.setCellStyle(style);
            c1.setCellStyle(style);
        }
        return rowIdx + 1;
    }

    private static CellStyle headerStyle(Workbook wb) {
        Font bold = wb.createFont();
        bold.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
