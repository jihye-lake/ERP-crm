package com.nexora.erp.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** 통계 화면(/statistics) 모델 객체. 화면 상단 KPI 카드 + Chart.js에 넘길 JSON 문자열을 담습니다. */
public class StatisticsView {

    private String selectedYear;
    private List<String> availableYears;
    private BigDecimal yearRevenue;
    private BigDecimal yearDirectCost;
    private String yearCostPercent;
    private long projectCount;
    private BigDecimal targetAmount;
    private BigDecimal achievementPercent;
    private String chartDataJson;

    public String getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(String selectedYear) {
        this.selectedYear = selectedYear;
    }

    public List<String> getAvailableYears() {
        return availableYears;
    }

    public void setAvailableYears(List<String> availableYears) {
        this.availableYears = availableYears;
    }

    public BigDecimal getYearRevenue() {
        return yearRevenue;
    }

    public void setYearRevenue(BigDecimal yearRevenue) {
        this.yearRevenue = yearRevenue;
    }

    public BigDecimal getYearDirectCost() {
        return yearDirectCost;
    }

    public void setYearDirectCost(BigDecimal yearDirectCost) {
        this.yearDirectCost = yearDirectCost;
    }

    public String getYearCostPercent() {
        return yearCostPercent;
    }

    public void setYearCostPercent(String yearCostPercent) {
        this.yearCostPercent = yearCostPercent;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getAchievementPercent() {
        return achievementPercent;
    }

    public void setAchievementPercent(BigDecimal achievementPercent) {
        this.achievementPercent = achievementPercent;
    }

    public String getChartDataJson() {
        return chartDataJson;
    }

    public void setChartDataJson(String chartDataJson) {
        this.chartDataJson = chartDataJson;
    }
}
