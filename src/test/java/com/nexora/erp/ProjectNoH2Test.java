package com.nexora.erp;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ProjectListRow;
import com.nexora.erp.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * h2 프로필에서 No(프로젝트 번호) + Kcode 제거(schema-h2-v5-data.sql) 확인.
 * PostgreSQL 쪽 V5 마이그레이션은 PostgresIntegrationTest 에서 같은 내용을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ProjectNoH2Test {

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 시드는_시간순으로_1번부터_빈틈없이_번호가_매겨진다() {
        List<Long> nos = jdbc.queryForList(
                "select no from project order by year_code, month_code, created_at, project_code", Long.class);

        assertThat(nos).hasSize(36);
        assertThat(nos).first().isEqualTo(1L);
        assertThat(nos).isSorted().doesNotHaveDuplicates();
        assertThat(nos).last().isEqualTo(36L);
    }

    @Test
    void 삭제해도_번호를_당기거나_재사용하지_않는다() {
        Project a = projectRepository.saveAndFlush(newProject("27-DSH-BOT-01"));
        Project b = projectRepository.saveAndFlush(newProject("27-DSH-BOT-02"));
        Project c = projectRepository.saveAndFlush(newProject("27-DSH-BOT-03"));
        assertThat(a.getNo()).isNotNull();
        assertThat(b.getNo()).isEqualTo(a.getNo() + 1);
        assertThat(c.getNo()).isEqualTo(a.getNo() + 2);

        // 가운데(b)와 마지막(c)을 삭제
        projectRepository.delete(b);
        projectRepository.delete(c);
        projectRepository.flush();

        Project d = projectRepository.saveAndFlush(newProject("27-DSH-BOT-04"));

        assertThat(jdbc.queryForObject("select no from project where project_code = '27-DSH-BOT-01'", Long.class))
                .isEqualTo(a.getNo());                     // 남은 번호는 그대로
        assertThat(d.getNo()).isEqualTo(a.getNo() + 3);   // 지워진 b, c 번호는 다시 쓰지 않음
    }

    @Test
    void 키워드_검색에서_No로_찾을_수_있다() {
        Long no = jdbc.queryForObject("select no from project where project_code = '26-DSH-BOT-03'", Long.class);

        List<ProjectListRow> rows = projectRepository.searchByKeyword(String.valueOf(no), PageRequest.of(0, 100)).getContent();

        assertThat(rows).extracting(ProjectListRow::getProjectCode).contains("26-DSH-BOT-03");
        assertThat(rows).filteredOn(r -> r.getProjectCode().equals("26-DSH-BOT-03"))
                .singleElement().extracting(ProjectListRow::getNo).isEqualTo(no);
    }

    @Test
    void Kcode_컬럼은_더_이상_없다() {
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'PROJECT' " +
                "and column_name in ('KCODE', 'REQUIRES_PARTNER_CODE')", Long.class)).isZero();
    }

    private static Project newProject(String code) {
        Project p = Project.newProject();
        p.setProjectCode(code);
        p.setMonthCode("03");
        p.setTitle("No 확인용 " + code);
        p.setRevenueAmount(BigDecimal.TEN);
        return p;
    }
}
