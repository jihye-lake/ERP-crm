package com.nexora.erp.repository;

import java.math.BigDecimal;

/**
 * 대시보드 목록/모니터 화면에서 사용하는 조회 전용 프로젝션.
 * 원본 index.asp의 "SELECT *, (직접비 서브쿼리) FROM ERP_TProject ..." 결과 행에 대응합니다.
 */
public interface ProjectListRow {
    /** 프로젝트 UUID의 문자열 표현 (화면에서 data-id, URL 경로 변수로만 사용) */
    String getId();
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
