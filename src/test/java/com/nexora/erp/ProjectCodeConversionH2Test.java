package com.nexora.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * h2 프로필에서 시드(옛 코드) → 새 프로젝트코드 변환(schema-h2-v4-data.sql) 확인.
 * PostgreSQL 쪽 V4 마이그레이션은 PostgresIntegrationTest 에서 같은 내용을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("h2")
class ProjectCodeConversionH2Test {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 시드_코드가_새_형식으로_바뀌고_월은_보존된다() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select project_code, year_code, month_code, title from project " +
                "where project_code like '26-DSH-BOT-%' order by project_code");

        assertThat(rows).extracting(r -> r.get("PROJECT_CODE"))
                .containsExactly("26-DSH-BOT-01", "26-DSH-BOT-02", "26-DSH-BOT-03", "26-DSH-BOT-04");
        assertThat(rows).extracting(r -> r.get("MONTH_CODE")).containsExactly("04", "09", "11", "11");
        assertThat(rows).extracting(r -> r.get("YEAR_CODE")).containsOnly("2026");
        assertThat(rows.get(2).get("TITLE")).isEqualTo("B2B SaaS 도입 의사결정 대시보드구축");
        assertThat(rows.get(3).get("TITLE")).isEqualTo("제로웨이스트 소비 행태 대시보드구축");
    }

    @Test
    void 옛_형식_코드는_남지_않고_전부_새_형식이다() {
        assertThat(jdbc.queryForObject("select count(*) from project", Long.class)).isEqualTo(36);
        assertThat(jdbc.queryForObject(
                "select count(*) from project where not regexp_like(project_code, '^[0-9]{2}-[A-Z]{2,3}-[A-Z]{3}-[0-9]{2}$')",
                Long.class)).isZero();
    }
}
