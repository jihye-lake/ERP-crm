package com.nexora.erp.domain;

/**
 * 원본 ASP의 Session(PROJECTCODE & "_AdminId") 값(0/1/2)을 대체하는 역할.
 * <p>
 * 원본 매핑: 0 = 팀장님(전체 권한), 1 = 실무 담당자(조회+직접비 입력),
 * 2 = 외부 협력사(제한된 조회 전용). 원본에서는 IP 화이트리스트로 자동 로그인되거나
 * 평문 비밀번호가 소스코드에 하드코딩되어 있었는데, 이 재구현에서는 그런 우회 로직 없이
 * DB 계정 + BCrypt 해시 + Spring Security 권한 체계로 완전히 대체했습니다.
 */
public enum AdminRole {

    /** 전체 권한: 프로젝트 업로드/삭제, 매출·Kcode 인라인 수정, 직접비 입력 */
    ADMIN("관리자"),

    /** 조회 + 직접비 입력 권한 (매출 수정/삭제/업로드 불가) */
    EDITOR("운영 담당자"),

    /** 제한된 조회 전용 (담당자/종류 컬럼 비노출, 직접비는 읽기 전용) */
    VIEWER("파트너사");

    private final String displayLabel;

    AdminRole(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
