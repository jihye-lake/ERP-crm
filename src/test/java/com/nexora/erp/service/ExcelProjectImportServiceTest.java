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

    private static final Object[] HEADER = {"Kcode", "프로젝트코드", "제목", "종류", "제작 담당자", "클라이언트", "클라이언트 담당자", "금액"};

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
    void 정상_행은_모두_저장되고_이력이_남는다() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());

        ExcelImportResult result = service.importFrom(xlsx(
                row("K26-01", "P26090001", "신규 대시보드 구축", "대시보드구축", "김도윤", "블루오션테크", "정민준", 12_000_000),
                row("", "p26090002", "데이터 분석", "데이터분석", "박서연", "그린필드전자", "한소율", "￦3,500,000")
        ));

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.savedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).saveAll(captor.capture());
        List<Project> saved = captor.getValue();
        assertThat(saved).extracting(Project::getProjectCode).containsExactly("P26090001", "P26090002"); // 소문자 → 대문자
        assertThat(saved.get(0).getRevenueAmount()).isEqualByComparingTo("12000000");
        assertThat(saved.get(1).getRevenueAmount()).isEqualByComparingTo("3500000"); // 통화기호/콤마 허용
        assertThat(saved.get(1).getKcode()).isNull();                                // 빈 Kcode는 null

        verify(auditLogService, times(2)).record(any(), any(), any(), any());
    }

    @Test
    void 빈_행은_오류가_아니라_무시한다() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of());

        ExcelImportResult result = service.importFrom(xlsx(
                row("", "P26090001", "제목", "", "", "", "", 1000),
                row("", "", "", "", "", "", "", ""),
                row("", "P26090002", "제목2", "", "", "", "", 2000)
        ));

        assertThat(result.savedCount()).isEqualTo(2);
    }

    @Test
    void 문제_행이_하나라도_있으면_전부_저장하지_않고_행번호와_사유를_돌려준다() throws IOException {
        ExcelImportResult result = service.importFrom(xlsx(
                row("", "P26090001", "정상 행", "", "", "", "", 1000),    // 엑셀 2행
                row("", "", "코드 없음", "", "", "", "", 1000),            // 3행
                row("", "X2609", "코드 형식 오류", "", "", "", "", 1000),   // 4행
                row("", "P26130001", "13월", "", "", "", "", 1000),       // 5행 - 월이 13
                row("", "P26090001", "파일 내 중복", "", "", "", "", 1000), // 6행
                row("", "P26090003", "", "", "", "", "", 1000),           // 7행 - 제목 없음
                row("", "P26090004", "금액 오류", "", "", "", "", "미정")   // 8행
        ));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.savedCount()).isZero();
        assertThat(result.errors()).extracting(RowError::rowNumber).containsExactly(3, 4, 5, 6, 7, 8);
        assertThat(result.errors().get(3).reason()).contains("2행");
        assertThat(result.errors().get(5).reason()).contains("미정");

        verify(projectRepository, never()).saveAll(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void 이미_DB에_있는_프로젝트코드는_오류() throws IOException {
        when(projectRepository.findExistingProjectCodes(anyCollection())).thenReturn(List.of("P26090002"));

        ExcelImportResult result = service.importFrom(xlsx(
                row("", "P26090001", "신규", "", "", "", "", 1000),
                row("", "P26090002", "기존", "", "", "", "", 1000)
        ));

        assertThat(result.errors()).singleElement().satisfies(err -> {
            assertThat(err.rowNumber()).isEqualTo(3);
            assertThat(err.reason()).contains("이미 등록된");
        });
        verify(projectRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------------

    private static Object[] row(Object... cells) {
        return cells;
    }

    private static InputStream xlsx(Object[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("projects");
            writeRow(sheet.createRow(0), HEADER);
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
