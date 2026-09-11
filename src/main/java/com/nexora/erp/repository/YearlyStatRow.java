package com.nexora.erp.repository;

import java.math.BigDecimal;

/** 통계 화면의 연도별 매출/직접비 추이 차트용 프로젝션. */
public interface YearlyStatRow {
    String getYearCode();
    BigDecimal getRevenue();
    BigDecimal getDirectCost();
}
