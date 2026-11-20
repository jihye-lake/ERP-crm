package com.nexora.erp;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.MonthlyStatRow;
import com.nexora.erp.repository.ProjectListRow;
import com.nexora.erp.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    void V4_기존_코드가_연도_종류_클라이언트별_시간순_순번으로_바뀌고_월은_보존된다() {
        // 2026년 대시보드구축 + 블루오션테크: 옛 코드 P26040002, P26090002, P26110001, P26110002
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select project_code, month_code, title from project where project_code like '26-DSH-BOT-%' order by project_code");

        assertThat(rows).extracting(r -> r.get("project_code"))
                .containsExactly("26-DSH-BOT-01", "26-DSH-BOT-02", "26-DSH-BOT-03", "26-DSH-BOT-04");
        assertThat(rows).extracting(r -> r.get("month_code")).containsExactly("04", "09", "11", "11");
        assertThat(rows.get(2).get("title")).isEqualTo("B2B SaaS 도입 의사결정 대시보드구축");
        assertThat(rows.get(3).get("title")).isEqualTo("제로웨이스트 소비 행태 대시보드구축");

        assertThat(jdbc.queryForObject("select count(*) from project where project_code like 'P%'", Long.class)).isZero();
    }

    @Test
    void V5_Kcode_컬럼이_삭제되고_기존_행은_시간순으로_1번부터_번호가_매겨진다() {
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'project' " +
                "and column_name in ('kcode', 'requires_partner_code')", Long.class)).isZero();

        List<Long> nos = jdbc.queryForList(
                "select no from project order by year_code, month_code, created_at, project_code", Long.class);
        assertThat(nos).hasSize(36).isSorted().doesNotHaveDuplicates();
        assertThat(nos).first().isEqualTo(1L);
        assertThat(nos).last().isEqualTo(36L);
    }

    @Test
    void No는_삭제돼도_당겨지거나_재사용되지_않는다() {
        Project a = projectRepository.saveAndFlush(newProject("27-DSH-BOT-01"));
        Project b = projectRepository.saveAndFlush(newProject("27-DSH-BOT-02"));
        Project c = projectRepository.saveAndFlush(newProject("27-DSH-BOT-03"));
        assertThat(b.getNo()).isEqualTo(a.getNo() + 1);
        assertThat(c.getNo()).isEqualTo(a.getNo() + 2);

        projectRepository.delete(b);
        projectRepository.delete(c);
        projectRepository.flush();
        Project d = projectRepository.saveAndFlush(newProject("27-DSH-BOT-04"));

        assertThat(d.getNo()).isEqualTo(a.getNo() + 3);
        assertThat(jdbc.queryForObject("select no from project where project_code = '27-DSH-BOT-01'", Long.class))
                .isEqualTo(a.getNo());
    }

    @Test
    void No는_수동으로_바꿀_수_없다() {
        // GENERATED ALWAYS: 지정 INSERT/UPDATE 모두 DB가 거부
        assertThatThrownBy(() -> jdbc.update("update project set no = 9999 where project_code = '26-DSH-BOT-01'"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void 프로젝트코드_앞_2자리에서_연도_컬럼이_만들어진다() {
        Project project = Project.newProject();
        project.setProjectCode("27-DSH-BOT-01");
        project.setMonthCode("03");
        project.setTitle("generated column 확인용");
        project.setRevenueAmount(BigDecimal.TEN);
        projectRepository.saveAndFlush(project);

        var row = jdbc.queryForMap("select year_code, month_code from project where project_code = '27-DSH-BOT-01'");
        assertThat(row).containsEntry("year_code", "2027").containsEntry("month_code", "03");
    }

    @Test
    void 형식이_틀린_프로젝트코드는_DB에서_거부된다() {
        Project project = Project.newProject();
        project.setProjectCode("P27030001");
        project.setMonthCode("03");
        project.setTitle("옛 형식");
        project.setRevenueAmount(BigDecimal.TEN);

        assertThatThrownBy(() -> projectRepository.saveAndFlush(project))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 연월_검색_결과의_id는_UUID_문자열이다() {
        Page<ProjectListRow> page = projectRepository.searchByYearMonth("2026", "08", PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allSatisfy(r -> assertThat(UUID.fromString(r.getId())).isNotNull());
    }

    @Test
    void 키워드_검색은_대소문자를_구분하지_않는다() {
        Page<ProjectListRow> page = projectRepository.searchByKeyword("26-dsh-bot", PageRequest.of(0, 100));

        assertThat(page.getContent()).extracting(ProjectListRow::getProjectCode)
                .containsExactly("26-DSH-BOT-01", "26-DSH-BOT-02", "26-DSH-BOT-03", "26-DSH-BOT-04");
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
    void 키워드_검색에서_No로_찾을_수_있다() {
        Long no = jdbc.queryForObject("select no from project where project_code = '26-DSH-BOT-03'", Long.class);

        Page<ProjectListRow> page = projectRepository.searchByKeyword(String.valueOf(no), PageRequest.of(0, 100));

        assertThat(page.getContent()).filteredOn(r -> r.getProjectCode().equals("26-DSH-BOT-03"))
                .singleElement().extracting(ProjectListRow::getNo).isEqualTo(no);
    }

    @Test
    void 엑셀_업로드_중복체크용_코드_조회() {
        assertThat(projectRepository.findExistingProjectCodes(List.of("26-DSH-BOT-03", "99-DSH-BOT-01")))
                .containsExactly("26-DSH-BOT-03");
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
