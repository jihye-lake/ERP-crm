package com.nexora.erp.web;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.service.DirectCostService;
import com.nexora.erp.web.dto.DirectCostForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 원본 DirectCost/index.asp(입력, 팀장/담당자), DirectCost/Check.asp(조회, 파트너사 포함 전체),
 * DirectCost/DirectCost_Pro.asp(저장) 세 파일에 대응.
 */
@Controller
@RequestMapping("/direct-cost")
public class DirectCostController {

    private final DirectCostService directCostService;
    private final ProjectRepository projectRepository;

    public DirectCostController(DirectCostService directCostService, ProjectRepository projectRepository) {
        this.directCostService = directCostService;
        this.projectRepository = projectRepository;
    }

    /** 원본 DirectCost/index.asp: 직접비 입력(편집) 화면 */
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String edit(@PathVariable UUID projectId, Model model, HttpSession session) {
        loadCommon(projectId, model, session);
        model.addAttribute("readOnly", false);
        return "direct-cost/edit";
    }

    /**
     * 원본 DirectCost/Check.asp: 직접비 조회(읽기 전용) 화면.
     * 원본도 AId(0,1) 외 권한은 접근이 차단되어 있어(파트너사/VIEWER는 열람 불가) 동일하게 제한합니다.
     */
    @GetMapping("/{projectId}/view")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String view(@PathVariable UUID projectId, Model model, HttpSession session) {
        loadCommon(projectId, model, session);
        model.addAttribute("readOnly", true);
        return "direct-cost/view";
    }

    /** 원본 DirectCost/DirectCost_Pro.asp: 저장(upsert) */
    @PostMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String save(@PathVariable UUID projectId, @ModelAttribute DirectCostForm form, Model model) {
        BigDecimal total = directCostService.save(projectId, form.getItems());
        model.addAttribute("total", total);
        return "direct-cost/saved";
    }

    private void loadCommon(UUID projectId, Model model, HttpSession session) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다: " + projectId));

        DirectCostForm form = new DirectCostForm();
        form.setProjectId(projectId);
        form.setItems(directCostService.loadForProject(projectId));

        model.addAttribute("project", project);
        model.addAttribute("form", form);
        model.addAttribute("total", directCostService.totalFor(projectId));
        model.addAttribute("timeoutSeconds", SessionTimeUtil.remainingSeconds(session));
    }
}
