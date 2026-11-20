package com.nexora.erp.web;

import com.nexora.erp.service.ExcelImportResult;
import com.nexora.erp.service.ExcelProjectImportService;
import com.nexora.erp.service.ExcelTemplateService;
import com.nexora.erp.service.ProjectService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.EmptyFileException;
import org.apache.poi.UnsupportedFileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 원본 Project_Upload.asp / Project_Delete.asp / Update_Cost.asp에 대응하는
 * 관리자 전용(팀장, ADMIN) 액션 엔드포인트. 프런트엔드 JS와의 호환을 위해
 * 원본과 동일하게 "Y" / "N" 텍스트 응답 계약을 유지합니다.
 * <p>
 * 업로드(upload)는 뷰 리졸버를 통한 redirect:가 필요해 일반 @Controller로 두고,
 * AJAX 응답(Y/N)이 필요한 나머지 메서드에만 개별적으로 @ResponseBody를 붙였습니다.
 */
@Controller
public class ProjectAdminController {

    private static final Logger log = LoggerFactory.getLogger(ProjectAdminController.class);

    private final ProjectService projectService;
    private final ExcelProjectImportService excelProjectImportService;
    private final ExcelTemplateService excelTemplateService;

    public ProjectAdminController(ProjectService projectService, ExcelProjectImportService excelProjectImportService,
                                  ExcelTemplateService excelTemplateService) {
        this.projectService = projectService;
        this.excelProjectImportService = excelProjectImportService;
        this.excelTemplateService = excelTemplateService;
    }

    /** 일괄 업로드용 엑셀 양식 다운로드 */
    @GetMapping("/projects/upload-template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> uploadTemplate() throws IOException {
        byte[] body = excelTemplateService.projectUploadTemplate();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("프로젝트_업로드_양식.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /** 원본 Project_Upload.asp */
    @PostMapping("/projects/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public String upload(@RequestParam("Project_Files") MultipartFile file,
                         @RequestParam(value = "month", required = false) String month,
                         RedirectAttributes redirect) {
        try {
            ExcelImportResult result = excelProjectImportService.importFrom(file, month);
            redirect.addFlashAttribute("importResult", result);
        } catch (IOException | UnsupportedFileFormatException | EmptyFileException e) {
            // 엑셀이 아니거나 깨진 파일 (POI가 읽지 못함)
            log.warn("엑셀 업로드 파일을 읽지 못함: {}", file.getOriginalFilename(), e);
            redirect.addFlashAttribute("importFileError", "엑셀 파일을 읽을 수 없습니다. .xls / .xlsx 파일인지 확인해 주세요.");
        }
        return "redirect:/";
    }

    /** 원본 Project_Delete.asp */
    @PostMapping(value = "/projects/{id}/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public String delete(@PathVariable UUID id) {
        try {
            return projectService.deleteProject(id) ? "Y" : "N";
        } catch (RuntimeException e) {
            log.error("프로젝트 삭제 실패: {}", id, e);
            return "N";
        }
    }

    /** 원본 Update_Cost.asp (원본처럼 통화기호/콤마가 섞여 들어와도 허용하도록 숫자만 추출) */
    @PostMapping(value = "/projects/{id}/revenue", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public String updateRevenue(@PathVariable UUID id, @RequestParam("Cost") String cost) {
        String numeric = cost == null ? "" : cost.replaceAll("[^0-9.\\-]", "");
        if (numeric.isBlank()) {
            return "N";
        }
        try {
            return projectService.updateRevenueAmount(id, new BigDecimal(numeric)) ? "Y" : "N";
        } catch (NumberFormatException e) {
            return "N";
        }
    }
}
