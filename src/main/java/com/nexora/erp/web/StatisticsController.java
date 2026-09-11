package com.nexora.erp.web;

import com.nexora.erp.domain.AdminAccount;
import com.nexora.erp.domain.AdminRole;
import com.nexora.erp.service.CurrentUserService;
import com.nexora.erp.service.StatisticsService;
import com.nexora.erp.web.dto.StatisticsView;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 통계 화면. 원본 ASP에는 없던 신규 화면으로, 매출/직접비 현황을 월별·연도별·클라이언트별·
 * 항목별 그래프로 한눈에 볼 수 있도록 대시보드 목록/모니터 화면에서 이동할 수 있는 버튼을 추가했습니다.
 * 매출·클라이언트 상세가 노출되므로 대시보드 전체 화면과 동일하게 팀장(ADMIN)/담당자(EDITOR)만 접근할 수 있습니다.
 */
@Controller
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final CurrentUserService currentUserService;

    public StatisticsController(StatisticsService statisticsService, CurrentUserService currentUserService) {
        this.statisticsService = statisticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String statistics(@RequestParam(value = "year", required = false) String year,
                              Authentication authentication, HttpSession session, Model model) {
        AdminAccount currentAdmin = currentUserService.resolve(authentication);
        StatisticsView view = statisticsService.build(year);

        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("isAdmin", currentAdmin.getRole() == AdminRole.ADMIN);
        model.addAttribute("canManageCost", currentAdmin.getRole() != AdminRole.VIEWER);
        model.addAttribute("stats", view);
        model.addAttribute("timeoutSeconds", SessionTimeUtil.remainingSeconds(session));
        return "statistics/view";
    }
}
