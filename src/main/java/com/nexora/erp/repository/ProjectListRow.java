package com.nexora.erp.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 대시보드 목록/모니터 화면에서 사용하는 조회 전용 프로젝션.
 * 원본 index.asp의 "SELECT *, (직접비 서브쿼리) FROM ERP_TProject ..." 결과 행에 대응합니다.
 */
public interface ProjectListRow {
    UUID getId();
    String getProjectCode();
    String getKcode();
    String getTitle();
    String getProjectType();
    String getManager();
    String getClientName();
    Boolean getRequiresPartnerCode();
    BigDecimal getRevenueAmount();
    BigDecimal getDirectCostTotal();
}
