package com.nexora.erp.service;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.service.ExcelImportResult.RowError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelProjectImportServiceTest {

    /** 예전 8열 양식 (헤더 F열 "클라이언트 담당자"로 구분) */
    private static final Object[] LEGACY_HEADER = {"프로젝트코드", "제목", "종류", "제작 담당자", "클라이언트", "클라이언트 담당자", "금액", "월"};

    @Mock
    ProjectRepository projectRepository;
    @Mock
    AuditLogService auditLogService;

    ExcelProjectImportService service;

    @BeforeEach
    void setUp() {
        service = new ExcelProjectImportService(projectRepository, auditLogService);
    }

    @Test
    void 예전_8열_양식_정상_행은_모두_저장되고_이력이_남는다() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());

        ExcelImportResult result = service.importFrom(legacyXlsx(
                row("26-DSH-BOT-05", "신규 대시보드 구축", "대시보드구축", "김도윤", "블루오션테크", "정민준", 12_000_000, 9),
                row("26-anl-gfe-03", "데이터 분석", "데이터분석", "박서연", "그린필드전자", "한소율", "￦3,500,000", "10월")
        ), null);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.savedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).saveAll(captor.capture());
        List<Project> saved = captor.getValue();
        assertThat(saved).extracting(Project::getProjectCode).containsExactly("26-DSH-BOT-05", "26-ANL-GFE-03"); // 소문자 → 대문자
        assertThat(saved).extracting(Project::getMonthCode).containsExactly("09", "10");                         // 9 / "10월" → 2자리
        assertThat(saved.get(0).getRevenueAmount()).isEqualByComparingTo("12000000");
        assertThat(saved.get(1).getRevenueAmount()).isEqualByComparingTo("3500000"); // 통화기호/콤마 허용

        verify(auditLogService, times(2)).record(any(), any(), any(), any());
    }

    @Test
    void 빈_행은_오류가_아니라_무시한다() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());

        ExcelImportResult result = service.importFrom(legacyXlsx(
                row("26-DSH-BOT-05", "제목", "", "", "", "", 1000, 1),
                row("", "", "", "", "", "", "", ""),
                row("26-DSH-BOT-06", "제목2", "", "", "", "", 2000, "02")
        ), null);

        assertThat(result.savedCount()).isEqualTo(2);
    }

    @Test
    void 문제_행이_하나라도_있으면_전부_저장하지_않고_행번호와_사유를_돌려준다() throws IOException {
        ExcelImportResult result = service.importFrom(legacyXlsx(
                row("26-DSH-BOT-05", "정상 행", "", "", "", "", 1000, 9),       // 엑셀 2행
                row("", "코드 없음", "", "", "", "", 1000, 9),                  // 3행
                row("P26090001", "옛 형식 코드", "", "", "", "", 1000, 9),      // 4행
                row("26-DASH-BOT-01", "종류 약어 4자리", "", "", "", "", 1000, 9), // 5행
                row("26-DSH-BOT-05", "파일 내 중복", "", "", "", "", 1000, 9),   // 6행
                row("26-DSH-BOT-07", "", "", "", "", "", 1000, 9),              // 7행 - 제목 없음
                row("26-DSH-BOT-08", "금액 오류", "", "", "", "", "미정", 9),    // 8행
                row("26-DSH-BOT-09", "월 없음", "", "", "", "", 1000, ""),      // 9행
                row("26-DSH-BOT-10", "13월", "", "", "", "", 1000, 13)          // 10행
        ), null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.savedCount()).isZero();
        assertThat(result.errors()).extracting(RowError::rowNumber).containsExactly(3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(result.errors().get(1).reason()).contains("26-DSH-BOT-01"); // 형식 안내에 새 예시
        assertThat(result.errors().get(3).reason()).contains("2행");
        assertThat(result.errors().get(5).reason()).contains("미정");
        assertThat(result.errors().get(6).reason()).contains("월이 비어");
        assertThat(result.errors().get(7).reason()).contains("13");

        verify(projectRepository, never()).saveAll(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void 이미_DB에_있는_프로젝트코드는_오류() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of("26-DSH-BOT-04"));

        ExcelImportResult result = service.importFrom(legacyXlsx(
                row("26-DSH-BOT-05", "신규", "", "", "", "", 1000, 11),
                row("26-DSH-BOT-04", "기존", "", "", "", "", 1000, 11)
        ), null);

        assertThat(result.errors()).singleElement().satisfies(err -> {
            assertThat(err.rowNumber()).isEqualTo(3);
            assertThat(err.reason()).contains("이미 등록된");
        });
        verify(projectRepository, never()).saveAll(any());
    }

    @Test
    void Kcode_열이_있는_예전_양식은_바로_안내하고_저장하지_않는다() throws IOException {
        ExcelImportResult result = service.importFrom(xlsxWithHeader(
                new Object[]{"Kcode", "프로젝트코드", "제목", "종류", "제작 담당자", "클라이언트", "클라이언트 담당자", "금액", "월"},
                row("K26-01", "26-DSH-BOT-05", "제목", "", "", "", "", 1000, 9)
        ), null);

        assertThat(result.errors()).singleElement().satisfies(err -> {
            assertThat(err.rowNumber()).isEqualTo(1);
            assertThat(err.reason()).contains("Kcode 열이 삭제");
        });
        verifyNoInteractions(projectRepository, auditLogService);
    }

    @Test
    void 새_양식은_6열이고_월은_화면에서_받은_값으로_들어간다() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());

        ExcelImportResult result = service.importFrom(xlsx(
                row("26-DSH-BOT-05", "신규 대시보드 구축", "대시보드구축", "김도윤", "블루오션테크", 12_000_000),
                row("26-anl-gfe-03", "데이터 분석", "데이터분석", "박서연", "그린필드전자", "￦3,500,000")
        ), "9");

        assertThat(result.hasErrors()).isFalse();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).saveAll(captor.capture());
        List<Project> saved = captor.getValue();
        assertThat(saved).extracting(Project::getProjectCode).containsExactly("26-DSH-BOT-05", "26-ANL-GFE-03");
        assertThat(saved).extracting(Project::getMonthCode).containsExactly("09", "09");
        assertThat(saved).extracting(Project::getClientName).containsExactly("블루오션테크", "그린필드전자");
        assertThat(saved).extracting(Project::getClientManager).containsOnlyNulls();
        assertThat(saved.get(0).getRevenueAmount()).isEqualByComparingTo("12000000");
        assertThat(saved.get(1).getRevenueAmount()).isEqualByComparingTo("3500000");
    }

    @Test
    void 새_양식인데_월을_모르면_저장하지_않는다() throws IOException {
        ExcelImportResult result = service.importFrom(xlsx(
                row("26-DSH-BOT-05", "제목", "", "", "", 1000)
        ), null);

        assertThat(result.errors()).singleElement().satisfies(err -> assertThat(err.reason()).contains("월"));
        verifyNoInteractions(projectRepository, auditLogService);
    }

    @Test
    void 다운로드한_양식을_그대로_채워_올리면_읽힌다() throws IOException {
        com.nexora.erp.repository.ClientRepository clients = mock(com.nexora.erp.repository.ClientRepository.class);
        when(clients.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        byte[] template = new ExcelTemplateService(clients).projectUploadTemplate();

        byte[] filled;
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("프로젝트");
            for (int c = 0; c < ExcelProjectImportService.TEMPLATE_HEADERS.size(); c++) {
                assertThat(sheet.getRow(0).getCell(c).getStringCellValue())
                        .isEqualTo(ExcelProjectImportService.TEMPLATE_HEADERS.get(c));
            }
            assertThat(sheet.getLastRowNum()).isZero(); // 예시 행 없음
            writeRow(sheet.createRow(1), row("26-PLT-SSH-01", "플랫폼", "플랫폼구축", "이하준", "스카이써니", 5_000_000));
            wb.write(out);
            filled = out.toByteArray();
        }

        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());
        ExcelImportResult result = service.importFrom(new ByteArrayInputStream(filled), "11");

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.savedCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------

    private static Object[] row(Object... cells) {
        return cells;
    }

    private static InputStream legacyXlsx(Object[]... rows) throws IOException {
        return xlsxWithHeader(LEGACY_HEADER, rows);
    }

    private static InputStream xlsx(Object[]... rows) throws IOException {
        return xlsxWithHeader(ExcelProjectImportService.TEMPLATE_HEADERS.toArray(), rows);
    }

    private static InputStream xlsxWithHeader(Object[] header, Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("projects");
            writeRow(sheet.createRow(0), header);
            for (int i = 0; i < rows.length; i++) {
                writeRow(sheet.createRow(i + 1), rows[i]);
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private static void writeRow(Row row, Object[] values) {
        for (int c = 0; c < values.length; c++) {
            Object v = values[c];
            if (v instanceof Number n) {
                row.createCell(c).setCellValue(n.doubleValue());
            } else if (v != null && !v.toString().isEmpty()) {
                row.createCell(c).setCellValue(v.toString());
            }
        }
    }
}
