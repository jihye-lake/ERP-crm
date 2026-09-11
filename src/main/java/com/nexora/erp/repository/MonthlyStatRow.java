package com.nexora.erp.repository;

import java.math.BigDecimal;

/** 통계 화면(/statistics)의 월별 매출/직접비 바 차트용 프로젝션. */
public interface MonthlyStatRow {
    String getMonthCode();
    BigDecimal getRevenue();
    BigDecimal getDirectCost();
}
