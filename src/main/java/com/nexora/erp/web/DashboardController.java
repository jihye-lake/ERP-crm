package com.nexora.erp.web;

import com.nexora.erp.domain.AdminAccount;
import com.nexora.erp.domain.AdminRole;
import com.nexora.erp.domain.ProjectType;
import com.nexora.erp.repository.ProjectListRow;
import com.nexora.erp.service.CurrentUserService;
import com.nexora.erp.service.ProjectService;
import com.nexora.erp.web.dto.ProjectSearchForm;
import com.nexora.erp.web.dto.ProjectSummary;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 원본 index.asp(전체 대시보드, 팀장/담당자 전용) + monitor.asp(제한된 조회, 전체 로그인 사용자)에 대응.
 */
@Controller
public class DashboardController {

    private static final List<String> MONTH_OPTIONS = List.of(
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public DashboardController(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    /** 원본 index.asp: 팀장(ADMIN)/담당자(EDITOR)만 접근 가능. 파트너사(VIEWER)는 접근 시 로그인 페이지로 튕겨나갔음 */
    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String fullDashboard(@ModelAttribute("form") ProjectSearchForm form,
                                 Authentication authentication, HttpSession session, Model model) {
        populateCommonModel(form, authentication, session, model);
        model.addAttribute("restrictedView", false);
        return "dashboard/list";
    }

    /** 원본 monitor.asp: 로그인한 모든 역할이 접근 가능한 제한된 조회 화면 */
    @GetMapping("/monitor")
    public String monitorDashboard(@ModelAttribute("form") ProjectSearchForm form,
                                    Authentication authentication, HttpSession session, Model model) {
        populateCommonModel(form, authentication, session, model);
        model.addAttribute("restrictedView", true);
        return "dashboard/monitor";
    }

    private void populateCommonModel(ProjectSearchForm form, Authentication authentication, HttpSession session, Model model) {
        if (form.getSearchType1() == null || form.getSearchType1().isBlank()) {
            ProjectSearchForm defaults = ProjectSearchForm.withDefaults();
            form.setSearchType1(defaults.getSearchType1());
            form.setSearchType2(defaults.getSearchType2());
        }

        AdminAccount currentAdmin = currentUserService.resolve(authentication);
        Page<ProjectListRow> page = projectService.search(form);
        ProjectSummary summary = projectService.summarize(form);

        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("isAdmin", currentAdmin.getRole() == AdminRole.ADMIN);
        model.addAttribute("canManageCost", currentAdmin.getRole() != AdminRole.VIEWER);
        model.addAttribute("canCreateProject",
                currentAdmin.getRole() == AdminRole.ADMIN || currentAdmin.getRole() == AdminRole.EDITOR);
        model.addAttribute("projectTypes", ProjectType.values());
        model.addAttribute("page", page);
        model.addAttribute("summary", summary);
        model.addAttribute("monthOptions", MONTH_OPTIONS);
        model.addAttribute("timeoutSeconds", SessionTimeUtil.remainingSeconds(session));
        model.addAttribute("yearCostPercent", PercentUtil.format(summary.getYearDirectCost(), summary.getYearRevenue()));
        model.addAttribute("monthCostPercent", PercentUtil.format(summary.getMonthDirectCost(), summary.getMonthRevenue()));
    }
}
