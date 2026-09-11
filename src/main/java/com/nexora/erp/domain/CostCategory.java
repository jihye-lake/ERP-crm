package com.nexora.erp.domain;

import java.util.List;

/**
 * 원본 ASP(DirectCost/index.asp)의 RCOD 문자열("101∮1-1. 항목명∏항목코드∑..." 형식)로
 * 하드코딩되어 있던 27개 직접비 항목 카탈로그를 열거형으로 정규화했습니다.
 * <p>
 * group 값은 원본의 "코드 앞자리(6,8 -> 파란색1 / 7,9 -> 파란색2)" 배경색 분기 로직을 그대로 유지하기 위한 값입니다.
 */
public enum CostCategory {

    PARTNER_COST_COMP("1-1. 파트너 정산비(완료)", 1),
    PARTNER_COST_OUT("1-2. 파트너 정산비(추가)", 1),
    PARTNER_COST_OVER("1-3. 파트너 정산비(초과)", 1),
    MEALS("2. 식대", 2),
    TRANS("3. 교통/숙박비", 3),
    BOOKS("4. 도서인쇄비", 4),
    INSURANCE("5. 보험료", 5),
    OUTSOURCE_1("6-1. 외주비용1", 6),
    OUTSOURCE_2("6-2. 외주비용2", 6),
    OUTSOURCE_3("6-3. 외주비용3", 6),
    OUTSOURCE_4("6-4. 외주비용4", 6),
    OUTSOURCE_5("6-5. 외주비용5", 6),
    GIVES_1("7-1. 프로모션/사은품비1", 7),
    GIVES_2("7-2. 프로모션/사은품비2", 7),
    GIVES_3("7-3. 프로모션/사은품비3", 7),
    GIVES_4("7-4. 프로모션/사은품비4", 7),
    GIVES_5("7-5. 프로모션/사은품비5", 7),
    PERSONS_1("8-1. 인건비1", 8),
    PERSONS_2("8-2. 인건비2", 8),
    PERSONS_3("8-3. 인건비3", 8),
    PERSONS_4("8-4. 인건비4", 8),
    PERSONS_5("8-5. 인건비5", 8),
    ETC_1("9-1. 기타1", 9),
    ETC_2("9-2. 기타2", 9),
    ETC_3("9-3. 기타3", 9),
    ETC_4("9-4. 기타4", 9),
    ETC_5("9-5. 기타5", 9);

    private final String label;
    private final int group;

    CostCategory(String label, int group) {
        this.label = label;
        this.group = group;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap 배경 강조 클래스 (원본의 bgColor 분기와 동일한 규칙) */
    public String getHighlightClass() {
        if (group == 6 || group == 8) {
            return "bg-blue1";
        }
        if (group == 7 || group == 9) {
            return "bg-blue2";
        }
        return "";
    }

    public static List<CostCategory> orderedList() {
        return List.of(values());
    }
}
