package com.nexora.erp.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatTest {

    final MoneyFormat money = new MoneyFormat();

    @ParameterizedTest
    @CsvSource({
            "192300000,  일억구천이백삼십만원",
            "7100000.00, 칠백십만원",
            "0,          영원",
            "1,          일원",
            "10,         십원",
            "1100,       천백원",
            "10000,      만원",
            "11000,      만천원",
            "21000,      이만천원",
            "100000000,  일억원",
            "100010000,  일억만원",
            "300000000,  삼억원",
            "1234567890, 십이억삼천사백오십육만칠천팔백구십원",
            "1000000000000, 일조원",
            "1500.99,    천오백원",
            "-5000,      마이너스 오천원"
    })
    void 한글_금액(String value, String expected) {
        assertThat(money.korean(new BigDecimal(value))).isEqualTo(expected);
    }

    @Test
    void 콤마() {
        assertThat(money.comma(new BigDecimal("7100000.00"))).isEqualTo("7,100,000");
        assertThat(money.comma(new BigDecimal("1234.5"))).isEqualTo("1,234.5");
        assertThat(money.comma(new BigDecimal("999"))).isEqualTo("999");
        assertThat(money.comma(null)).isEqualTo("0");
    }

    @Test
    void 변경이력_값() {
        assertThat(money.auditValue("7100000")).isEqualTo("7,100,000");
        assertThat(money.auditValue("2026 리포트 / 매출 7100000 / 직접비 0"))
                .isEqualTo("2026 리포트 / 매출 7,100,000 / 직접비 0");
        assertThat(money.auditKorean("7100000")).isEqualTo("칠백십만원");
        assertThat(money.auditKorean("제목 / 매출 1")).isNull();
        assertThat(money.auditValue(null)).isNull();
    }
}
