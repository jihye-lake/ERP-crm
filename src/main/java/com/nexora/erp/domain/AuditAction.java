package com.nexora.erp.domain;

public enum AuditAction {

    REVENUE_UPDATE("매출 수정"),
    /** Kcode는 V5에서 삭제됨. 과거 변경 이력 표시용으로만 남겨둔다 (새로 기록되지 않음) */
    KCODE_UPDATE("Kcode 수정"),
    DIRECT_COST_UPDATE("직접비 저장"),
    PROJECT_CREATE("프로젝트 추가"),
    PROJECT_DELETE("프로젝트 삭제"),
    EXCEL_IMPORT("엑셀 업로드");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
