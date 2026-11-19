package com.nexora.erp;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.MonthlyStatRow;
import com.nexora.erp.repository.ProjectListRow;
import com.nexora.erp.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영과 같은 PostgreSQL로 마이그레이션 + 네이티브 쿼리 검증.
 * <p>
 * H2로만 돌리면 통과하는데 PostgreSQL에서 터지는 경우(또는 반대)가 실제로 있었음
 * (generated column 문법, UUID 반환 타입 등) → 쿼리는 진짜 DB로 확인.
 * Docker가 없는 PC에서는 자동으로 skip 되고, CI(GitHub Actions)에서는 항상 돈다.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void Flyway_마이그레이션이_순서대로_적용된다() {
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void 프로젝트코드에서_연월_컬럼이_자동으로_만들어진다() {
        Project project = Project.newProject();
        project.setProjectCode("P27030001");
        project.setTitle("generated column 확인용");
        project.setRevenueAmount(BigDecimal.TEN);
        projectRepository.saveAndFlush(project);

        var row = jdbc.queryForMap("select year_code, month_code from project where project_code = 'P27030001'");
        assertThat(row).containsEntry("year_code", "2027").containsEntry("month_code", "03");
    }

    @Test
    void 연월_검색_결과의_id는_UUID_문자열이다() {
        Page<ProjectListRow> page = projectRepository.searchByYearMonth("2026", "08", PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allSatisfy(r -> assertThat(UUID.fromString(r.getId())).isNotNull());
    }

    @Test
    void 키워드_검색은_대소문자를_구분하지_않는다() {
        Page<ProjectListRow> page = projectRepository.searchByKeyword("p2601", PageRequest.of(0, 100));

        assertThat(page.getContent()).extracting(ProjectListRow::getProjectCode)
                .containsExactly("P26010001", "P26010002");
    }

    @Test
    void 월별_통계_합계가_원본_데이터_합계와_일치한다() {
        List<MonthlyStatRow> monthly = projectRepository.monthlyStatsForYear("2026");

        BigDecimal revenueSum = monthly.stream().map(MonthlyStatRow::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costSum = monthly.stream().map(MonthlyStatRow::getDirectCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(monthly).hasSize(12);
        assertThat(revenueSum).isEqualByComparingTo(jdbc.queryForObject(
                "select sum(revenue_amount) from project where year_code = '2026'", BigDecimal.class));
        assertThat(costSum).isEqualByComparingTo(jdbc.queryForObject(
                "select sum(d.amount) from direct_cost_item d join project p on p.id = d.project_id where p.year_code = '2026'",
                BigDecimal.class));
    }

    @Test
    void 엑셀_업로드_중복체크용_코드_조회() {
        assertThat(projectRepository.findExistingProjectCodes(List.of("P26010001", "P99990001")))
                .containsExactly("P26010001");
    }
}
