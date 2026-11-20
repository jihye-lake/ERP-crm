package com.nexora.erp.repository;

import java.math.BigDecimal;

/**
 * 대시보드 목록/모니터 화면에서 사용하는 조회 전용 프로젝션.
 * 원본 index.asp의 "SELECT *, (직접비 서브쿼리) FROM ERP_TProject ..." 결과 행에 대응합니다.
 */
public interface ProjectListRow {
    /** 프로젝트 UUID의 문자열 표현 (화면에서 data-id, URL 경로 변수로만 사용) */
    String getId();
    /** 프로젝트 번호(No). 삭제돼도 당겨지지 않는 영구 번호 */
    Long getNo();
    String getProjectCode();
    String getTitle();
    String getProjectType();
    String getManager();
    String getClientName();
    BigDecimal getRevenueAmount();
    BigDecimal getDirectCostTotal();
}
