package com.nexora.erp.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 화면 금액 표시: 천 단위 콤마 + 한글 금액(일억구천이백삼십만원).
 * 템플릿에서 {@code ${@money.comma(x)}}, {@code ${@money.korean(x)}} 로 사용.
 * 화면 JS 쪽 같은 규칙은 static/js/money.js (여기를 고치면 거기도 같이 맞출 것!)
 */
@Component("money")
public class MoneyFormat {

    private static final String[] DIGITS = {"", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"};
    private static final String[] SMALL_UNITS = {"", "십", "백", "천"};
    private static final String[] BIG_UNITS = {"", "만", "억", "조", "경", "해"};
    private static final BigInteger TEN_THOUSAND = BigInteger.valueOf(10_000);

    private static final Pattern PLAIN_NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern LABELED_AMOUNT = Pattern.compile("(매출|직접비) (-?\\d+(?:\\.\\d+)?)");

    /** 7100000.00 → "7,100,000" / 1234.5 → "1,234.5" / null → "0" */
    public String comma(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return new DecimalFormat("#,##0.##").format(value);
    }

    /**
     * 192300000 → "일억구천이백삼십만원". 원 미만은 버림, 0 → "영원".
     * 말하는 방식대로 십/백/천 앞의 '일'은 빼고(천백 원), '만' 단독일 때도 빼고(만 원), 억 이상은 붙인다(일억).
     */
    public String korean(BigDecimal value) {
        if (value == null) {
            return "영원";
        }
        BigInteger won = value.setScale(0, RoundingMode.DOWN).toBigInteger();
        if (won.signum() == 0) {
            return "영원";
        }
        String sign = won.signum() < 0 ? "마이너스 " : "";
        won = won.abs();

        StringBuilder result = new StringBuilder();
        int unitIndex = 0;
        while (won.signum() > 0 && unitIndex < BIG_UNITS.length) {
            int group = won.mod(TEN_THOUSAND).intValue();
            won = won.divide(TEN_THOUSAND);
            if (group > 0) {
                String groupText = (unitIndex == 1 && group == 1) ? "" : groupToKorean(group);
                result.insert(0, groupText + BIG_UNITS[unitIndex]);
            }
            unitIndex++;
        }
        return sign + result + "원";
    }

    /**
     * 변경 이력 값 표시용. 숫자만 있으면 콤마, "제목 / 매출 7100000 / 직접비 0" 같은 문장은 매출/직접비 뒤 숫자만 콤마.
     * (제목 안의 숫자 "2026 리포트" 같은 건 건드리지 않음)
     */
    public String auditValue(String value) {
        if (value == null) {
            return null;
        }
        if (PLAIN_NUMBER.matcher(value).matches()) {
            return comma(new BigDecimal(value));
        }
        Matcher m = LABELED_AMOUNT.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + " " + comma(new BigDecimal(m.group(2)))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 변경 이력 값이 숫자만 있을 때만 한글 금액, 아니면 null */
    public String auditKorean(String value) {
        return value != null && PLAIN_NUMBER.matcher(value).matches() ? korean(new BigDecimal(value)) : null;
    }

    private static String groupToKorean(int group) {
        StringBuilder sb = new StringBuilder();
        for (int pos = 3; pos >= 0; pos--) {
            int digit = group / (int) Math.pow(10, pos) % 10;
            if (digit == 0) {
                continue;
            }
            sb.append(digit == 1 && pos > 0 ? "" : DIGITS[digit]).append(SMALL_UNITS[pos]);
        }
        return sb.toString();
    }
}
