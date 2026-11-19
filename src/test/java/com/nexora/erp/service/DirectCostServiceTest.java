package com.nexora.erp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 원본 ASP 화면의 FuncSum() 자바스크립트 계산 규칙을 그대로 옮겼는지 확인.
 * "0이나 빈칸은 곱셈에서 빠진다(=1 취급)" 규칙이 헷갈려서 케이스별로 고정해둠.
 */
class DirectCostServiceTest {

    @ParameterizedTest(name = "단가={0}, 투여량={1}, 일자={2} → {3}")
    @CsvSource({
            "1000, 2,    3,    6000",
            "1000, 2,    ,     2000",   // 일자 빈칸 → 제외
            "1000, 0,    3,    3000",   // 투여량 0 → 제외
            "1000, ,     ,     1000",
            ",     ,     5,    5",
            "1500.5, 2,  ,     3001.0",
    })
    void 단가_투여량_일자_중_값이_있는_것만_곱한다(String unitPrice, String quantity, String days, String expected) {
        BigDecimal result = DirectCostService.computeAmount(dec(unitPrice), dec(quantity), dec(days));

        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("전부 비어 있거나 0이면 1이 아니라 0이 된다")
    void 전부_비어있으면_0() {
        assertThat(DirectCostService.computeAmount(null, null, null)).isEqualByComparingTo("0");
        assertThat(DirectCostService.computeAmount(BigDecimal.ZERO, BigDecimal.ZERO, null)).isEqualByComparingTo("0");
    }

    private static BigDecimal dec(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }
}
