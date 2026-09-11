package com.nexora.erp.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 원본 화면 하단의 "직접비/매출 x 100" 퍼센트 표시(JS로 계산하던 것)를 서버에서 안전하게 계산 */
public final class PercentUtil {

    private PercentUtil() {
    }

    public static String format(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0 || numerator == null) {
            return "0.0%";
        }
        BigDecimal percent = numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
        return percent + "%";
    }
}
