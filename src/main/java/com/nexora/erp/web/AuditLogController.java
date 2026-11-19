package com.nexora.erp.web;

import com.nexora.erp.domain.AdminAccount;
import com.nexora.erp.domain.AdminRole;
import com.nexora.erp.domain.ProjectAuditLog;
import com.nexora.erp.service.AuditLogService;
import com.nexora.erp.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 변경 이력 조회 (관리자 전용) */
@Controller
public class AuditLogController {

    private static final int PAGE_SIZE = 50;

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AuditLogController(AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public String list(@RequestParam(value = "projectCode", required = false) String projectCode,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       Authentication authentication, HttpSession session, Model model) {
        AdminAccount currentAdmin = currentUserService.resolve(authentication);
        Page<ProjectAuditLog> logs = auditLogService.search(projectCode, page - 1, PAGE_SIZE);

        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("isAdmin", currentAdmin.getRole() == AdminRole.ADMIN);
        model.addAttribute("canManageCost", currentAdmin.getRole() != AdminRole.VIEWER);
        model.addAttribute("logs", logs);
        model.addAttribute("projectCode", projectCode == null ? "" : projectCode);
        model.addAttribute("timeoutSeconds", SessionTimeUtil.remainingSeconds(session));
        return "audit/list";
    }
}
