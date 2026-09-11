package com.nexora.erp.repository;

import java.math.BigDecimal;

/** 통계 화면의 직접비 항목(카테고리)별 비중 차트용 프로젝션. */
public interface CategoryCostRow {
    String getCategoryCode();
    BigDecimal getAmount();
}
