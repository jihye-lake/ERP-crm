package com.nexora.erp.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 대시보드 상단 요약 박스(연 매출/직접비, 연+월 매출/직접비) + 연도 셀렉트 옵션.
 * (Thymeleaf/SpEL 프로퍼티 접근 호환성을 위해 record 대신 표준 getter를 가진 클래스로 작성)
 */
public class ProjectSummary {

    private final BigDecimal yearRevenue;
    private final BigDecimal yearDirectCost;
    private final BigDecimal monthRevenue;
    private final BigDecimal monthDirectCost;
    private final BigDecimal filteredRevenueTotal;
    private final BigDecimal filteredDirectCostTotal;
    private final List<String> availableYears;

    public ProjectSummary(BigDecimal yearRevenue, BigDecimal yearDirectCost, BigDecimal monthRevenue,
                           BigDecimal monthDirectCost, BigDecimal filteredRevenueTotal,
                           BigDecimal filteredDirectCostTotal, List<String> availableYears) {
        this.yearRevenue = yearRevenue;
        this.yearDirectCost = yearDirectCost;
        this.monthRevenue = monthRevenue;
        this.monthDirectCost = monthDirectCost;
        this.filteredRevenueTotal = filteredRevenueTotal;
        this.filteredDirectCostTotal = filteredDirectCostTotal;
        this.availableYears = availableYears;
    }

    public BigDecimal getYearRevenue() { return yearRevenue; }
    public BigDecimal getYearDirectCost() { return yearDirectCost; }
    public BigDecimal getMonthRevenue() { return monthRevenue; }
    public BigDecimal getMonthDirectCost() { return monthDirectCost; }
    public BigDecimal getFilteredRevenueTotal() { return filteredRevenueTotal; }
    public BigDecimal getFilteredDirectCostTotal() { return filteredDirectCostTotal; }
    public List<String> getAvailableYears() { return availableYears; }
}
