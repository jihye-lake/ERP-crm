package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.DirectCostItemRepository;
import com.nexora.erp.repository.ProjectListRow;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.web.dto.ProjectSearchForm;
import com.nexora.erp.web.dto.ProjectSummary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 원본 index.asp / monitor.asp의 검색/페이징/합계 계산 로직에 대응하는 서비스.
 * 문자열을 이어붙여 SQL을 조립하던 원본과 달리, 파라미터 바인딩된 쿼리를 사용합니다.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DirectCostItemRepository directCostItemRepository;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository, DirectCostItemRepository directCostItemRepository,
                          AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.directCostItemRepository = directCostItemRepository;
        this.auditLogService = auditLogService;
    }

    public Page<ProjectListRow> search(ProjectSearchForm form) {
        Pageable pageable = PageRequest.of(form.getCurPage() - 1, form.getPageSize());
        if (form.isKeywordMode()) {
            return projectRepository.searchByKeyword(form.getSearchProject().trim(), pageable);
        }
        return projectRepository.searchByYearMonth(normalizeYear(form.getSearchType1()), normalizeMonth(form.getSearchType2()), pageable);
    }

    public ProjectSummary summarize(ProjectSearchForm form) {
        String year = normalizeYear(form.getSearchType1());
        String month = normalizeMonth(form.getSearchType2());

        BigDecimal yearRevenue = nz(projectRepository.sumRevenueByYear(year));
        BigDecimal yearCost = nz(projectRepository.sumDirectCostByYear(year));
        BigDecimal monthRevenue = nz(projectRepository.sumRevenueByYearMonth(year, month));
        BigDecimal monthCost = nz(projectRepository.sumDirectCostByYearMonth(year, month));

        BigDecimal filteredRevenue;
        BigDecimal filteredCost;
        if (form.isKeywordMode()) {
            String keyword = form.getSearchProject().trim();
            filteredRevenue = nz(projectRepository.sumRevenueForKeywordFilter(keyword));
            filteredCost = nz(projectRepository.sumDirectCostForKeywordFilter(keyword));
        } else {
            filteredRevenue = nz(projectRepository.sumRevenueForYearMonthFilter(year, month));
            filteredCost = nz(projectRepository.sumDirectCostForYearMonthFilter(year, month));
        }

        List<String> years = projectRepository.findDistinctYears();
        return new ProjectSummary(yearRevenue, yearCost, monthRevenue, monthCost, filteredRevenue, filteredCost, years);
    }

    @Transactional
    public boolean deleteProject(UUID id) {
        return projectRepository.findById(id)
                .map(project -> {
                    auditLogService.record(project, AuditAction.PROJECT_DELETE,
                            project.getTitle() + " / 매출 " + plain(project.getRevenueAmount()), null);
                    directCostItemRepository.deleteByProjectId(id);
                    projectRepository.delete(project);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean updateRevenueAmount(UUID id, BigDecimal amount) {
        return projectRepository.findById(id)
                .map(project -> {
                    auditLogService.record(project, AuditAction.REVENUE_UPDATE,
                            plain(project.getRevenueAmount()), plain(amount));
                    project.setRevenueAmount(amount);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean updateKcode(UUID id, String kcode) {
        return projectRepository.findById(id)
                .map(project -> {
                    auditLogService.record(project, AuditAction.KCODE_UPDATE, project.getKcode(), kcode);
                    project.setKcode(kcode);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    /** 7100000.00 → "7100000" 처럼 이력에 보기 좋게 남기기 위함 */
    static String plain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalizeYear(String raw) {
        if (raw != null && raw.trim().length() == 4) {
            return raw.trim();
        }
        return String.valueOf(LocalDate.now().getYear());
    }

    private static String normalizeMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            return String.format("%02d", LocalDate.now().getMonthValue());
        }
        String trimmed = raw.trim();
        return trimmed.length() == 1 ? "0" + trimmed : trimmed;
    }
}
