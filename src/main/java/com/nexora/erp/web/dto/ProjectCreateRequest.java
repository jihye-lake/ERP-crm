package com.nexora.erp.web.dto;

import java.math.BigDecimal;

/**
 * 대시보드 목록의 "+" 행에서 보내는 프로젝트 추가 요청 (JSON).
 * 프로젝트코드는 받지 않는다 (서버에서 자동 생성). year/month 는 화면에서 보고 있던 연/월.
 */
public record ProjectCreateRequest(
        String title,
        String projectType,
        String manager,
        String clientName,
        BigDecimal revenueAmount,
        BigDecimal directCost,
        String year,
        String month) {
}
