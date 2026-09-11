package com.nexora.erp.service;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ProjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 원본 Project_Upload.asp의 엑셀 일괄 업로드 로직에 대응.
 * 원본은 IIS 전용 컴포넌트(DEXT.FileUpload) + ADO Jet OLEDB로 .xls 파일을 직접 읽었는데,
 * 이 재구현에서는 Spring MVC의 MultipartFile + Apache POI로 대체했습니다 (.xls, .xlsx 모두 지원).
 * <p>
 * 엑셀 컬럼 순서(1행은 헤더로 간주하고 건너뜀): Kcode, 프로젝트코드, 제목, 종류, 제작 담당자, 클라이언트, 클라이언트 담당자, 금액(부가세별도)
 */
@Service
public class ExcelProjectImportService {

    private final ProjectRepository projectRepository;

    public ExcelProjectImportService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public int importFrom(MultipartFile file) throws IOException {
        List<Project> parsed = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) { // 0행은 헤더라 건너뜀
                Row row = sheet.getRow(rowIdx);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                Project project = Project.newProject();
                project.setKcode(readString(row, 0, formatter));
                project.setProjectCode(readString(row, 1, formatter));
                project.setTitle(readString(row, 2, formatter));
                project.setProjectType(readString(row, 3, formatter));
                project.setManager(readString(row, 4, formatter));
                project.setClientName(readString(row, 5, formatter));
                project.setClientManager(readString(row, 6, formatter));
                project.setRevenueAmount(readAmount(row, 7, formatter));

                if (project.getProjectCode() == null || project.getProjectCode().isBlank()) {
                    continue; // 프로젝트코드가 없는 행은 무시
                }

                parsed.add(project);
            }
        }

        projectRepository.saveAll(parsed);
        return parsed.size();
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int c = 0; c <= 7; c++) {
            String value = readString(row, c, formatter);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readString(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private BigDecimal readAmount(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String raw = formatter.formatCellValue(cell).replaceAll("[^0-9.\\-]", "");
        if (raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
