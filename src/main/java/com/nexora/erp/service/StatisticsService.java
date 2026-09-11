package com.nexora.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.erp.domain.CostCategory;
import com.nexora.erp.repository.CategoryCostRow;
import com.nexora.erp.repository.DirectCostItemRepository;
import com.nexora.erp.repository.GroupRevenueRow;
import com.nexora.erp.repository.MonthlyStatRow;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.repository.YearlyStatRow;
import com.nexora.erp.web.PercentUtil;
import com.nexora.erp.web.dto.StatisticsView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 통계 화면(/statistics) 전용 서비스. 원본 ASP에는 없던 신규 화면으로,
 * 대시보드에 이미 쌓여 있는 매출/직접비 데이터를 여러 각도(월별/연도별/클라이언트별/항목별)로
 * 집계해 Chart.js용 데이터를 만듭니다.
 */
@Service
public class StatisticsService {

    private static final List<String> MONTHS =
            List.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");

    /** 클라이언트/항목별 도넛 차트에 표시할 최대 항목 수 (그 외는 "기타"로 묶음) */
    private static final int MAX_SLICES = 7;

    private final ProjectRepository projectRepository;
    private final DirectCostItemRepository directCostItemRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal annualTargetAmount;

    public StatisticsService(ProjectRepository projectRepository,
                              DirectCostItemRepository directCostItemRepository,
                              ObjectMapper objectMapper,
                              @Value("${erp.annual-target-amount:150000000}") long annualTargetAmount) {
        this.projectRepository = projectRepository;
        this.directCostItemRepository = directCostItemRepository;
        this.objectMapper = objectMapper;
        this.annualTargetAmount = BigDecimal.valueOf(annualTargetAmount);
    }

    public StatisticsView build(String requestedYear) {
        List<String> availableYears = projectRepository.findDistinctYears();
        String year = normalizeYear(requestedYear, availableYears);

        BigDecimal yearRevenue = nz(projectRepository.sumRevenueByYear(year));
        BigDecimal yearCost = nz(projectRepository.sumDirectCostByYear(year));
        long projectCount = projectRepository.countByYear(year);

        BigDecimal achievementPercent = annualTargetAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : yearRevenue.multiply(BigDecimal.valueOf(100)).divide(annualTargetAmount, 1, RoundingMode.HALF_UP);

        List<MonthlyStatRow> monthlyRows = projectRepository.monthlyStatsForYear(year);
        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("year", year);
        chartData.put("months", MONTHS);
        chartData.put("monthRevenue", monthlySeries(monthlyRows, true));
        chartData.put("monthDirectCost", monthlySeries(monthlyRows, false));

        List<GroupRevenueRow> clientRows = projectRepository.revenueByClientForYear(year);
        chartData.put("clientLabels", labelsOf(clientRows, MAX_SLICES));
        chartData.put("clientValues", valuesOf(clientRows, MAX_SLICES));

        List<GroupRevenueRow> typeRows = projectRepository.revenueByTypeForYear(year);
        chartData.put("typeLabels", labelsOf(typeRows, MAX_SLICES));
        chartData.put("typeValues", valuesOf(typeRows, MAX_SLICES));

        List<CategoryCostRow> categoryRows = directCostItemRepository.costByCategoryForYear(year);
        chartData.put("categoryLabels", categoryLabelsOf(categoryRows, MAX_SLICES));
        chartData.put("categoryValues", categoryValuesOf(categoryRows, MAX_SLICES));

        List<YearlyStatRow> yearlyRows = projectRepository.revenueByYear();
        chartData.put("yearLabels", yearlyRows.stream().map(YearlyStatRow::getYearCode).toList());
        chartData.put("yearRevenue", yearlyRows.stream().map(YearlyStatRow::getRevenue).toList());
        chartData.put("yearDirectCost", yearlyRows.stream().map(YearlyStatRow::getDirectCost).toList());

        chartData.put("targetAmount", annualTargetAmount);
        chartData.put("achievementPercent", achievementPercent);

        String chartDataJson = writeJson(chartData);

        StatisticsView view = new StatisticsView();
        view.setSelectedYear(year);
        view.setAvailableYears(availableYears);
        view.setYearRevenue(yearRevenue);
        view.setYearDirectCost(yearCost);
        view.setYearCostPercent(PercentUtil.format(yearCost, yearRevenue));
        view.setProjectCount(projectCount);
        view.setTargetAmount(annualTargetAmount);
        view.setAchievementPercent(achievementPercent);
        view.setChartDataJson(chartDataJson);
        return view;
    }

    private List<BigDecimal> monthlySeries(List<MonthlyStatRow> rows, boolean revenue) {
        Map<String, MonthlyStatRow> byMonth = new LinkedHashMap<>();
        for (MonthlyStatRow row : rows) {
            byMonth.put(row.getMonthCode(), row);
        }
        List<BigDecimal> series = new ArrayList<>();
        for (String month : MONTHS) {
            MonthlyStatRow row = byMonth.get(month);
            if (row == null) {
                series.add(BigDecimal.ZERO);
            } else {
                series.add(nz(revenue ? row.getRevenue() : row.getDirectCost()));
            }
        }
        return series;
    }

    /** 상위 N개는 그대로, 나머지는 "기타"로 합산해 도넛 차트 범례가 너무 길어지지 않도록 함 */
    private List<String> labelsOf(List<GroupRevenueRow> rows, int max) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), max); i++) {
            labels.add(rows.get(i).getLabel());
        }
        if (rows.size() > max) {
            labels.add("기타");
        }
        return labels;
    }

    private List<BigDecimal> valuesOf(List<GroupRevenueRow> rows, int max) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), max); i++) {
            values.add(nz(rows.get(i).getRevenue()));
        }
        if (rows.size() > max) {
            BigDecimal rest = BigDecimal.ZERO;
            for (int i = max; i < rows.size(); i++) {
                rest = rest.add(nz(rows.get(i).getRevenue()));
            }
            values.add(rest);
        }
        return values;
    }

    private List<String> categoryLabelsOf(List<CategoryCostRow> rows, int max) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), max); i++) {
            labels.add(labelOf(rows.get(i).getCategoryCode()));
        }
        if (rows.size() > max) {
            labels.add("기타");
        }
        return labels;
    }

    private List<BigDecimal> categoryValuesOf(List<CategoryCostRow> rows, int max) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), max); i++) {
            values.add(nz(rows.get(i).getAmount()));
        }
        if (rows.size() > max) {
            BigDecimal rest = BigDecimal.ZERO;
            for (int i = max; i < rows.size(); i++) {
                rest = rest.add(nz(rows.get(i).getAmount()));
            }
            values.add(rest);
        }
        return values;
    }

    private static String labelOf(String categoryCode) {
        try {
            return CostCategory.valueOf(categoryCode).getLabel();
        } catch (IllegalArgumentException e) {
            return categoryCode;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalizeYear(String raw, List<String> availableYears) {
        if (raw != null && raw.trim().length() == 4) {
            return raw.trim();
        }
        if (availableYears != null && !availableYears.isEmpty()) {
            return availableYears.get(0);
        }
        return String.valueOf(LocalDate.now().getYear());
    }
}
