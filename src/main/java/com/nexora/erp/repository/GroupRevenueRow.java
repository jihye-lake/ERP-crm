package com.nexora.erp.repository;

import java.math.BigDecimal;

/** 통계 화면의 클라이언트별/프로젝트 종류별 매출 비중 도넛 차트용 프로젝션. */
public interface GroupRevenueRow {
    String getLabel();
    BigDecimal getRevenue();
}
