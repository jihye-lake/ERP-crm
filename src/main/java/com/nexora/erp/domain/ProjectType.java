package com.nexora.erp.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 프로젝트 종류 + 프로젝트코드에 들어가는 약어 (26-<b>DSH</b>-BOT-01).
 * DB의 project.project_type 에는 지금처럼 한글 라벨이 그대로 저장된다.
 * V4 마이그레이션의 종류 → 약어 매핑과 같은 값이어야 함!
 */
public enum ProjectType {

    DASHBOARD("대시보드구축", "DSH"),
    PLATFORM("플랫폼구축", "PLT"),
    ANALYSIS("데이터분석", "ANL"),
    CONSULTING("컨설팅", "CON");

    private final String label;
    private final String abbr;

    ProjectType(String label, String abbr) {
        this.label = label;
        this.abbr = abbr;
    }

    public String getLabel() {
        return label;
    }

    public String getAbbr() {
        return abbr;
    }

    public static Optional<ProjectType> fromLabel(String label) {
        return Arrays.stream(values()).filter(t -> t.label.equals(label)).findFirst();
    }
}
