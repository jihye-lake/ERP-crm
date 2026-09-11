package com.nexora.erp.repository;

import com.nexora.erp.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    String LIST_SELECT =
            "select p.id as id, p.project_code as projectCode, p.kcode as kcode, p.title as title, " +
            "p.project_type as projectType, p.manager as manager, p.client_name as clientName, " +
            "p.requires_partner_code as requiresPartnerCode, p.revenue_amount as revenueAmount, " +
            "coalesce((select sum(d.amount) from direct_cost_item d where d.project_id = p.id), 0) as directCostTotal " +
            "from project p ";

    // ---- 기본 모드: 연/월 필터 (searchType1/searchType2) ----
    @Query(value = LIST_SELECT + "where p.year_code = :year and p.month_code = :month order by p.project_code asc",
            countQuery = "select count(*) from project p where p.year_code = :year and p.month_code = :month",
            nativeQuery = true)
    Page<ProjectListRow> searchByYearMonth(@Param("year") String year, @Param("month") String month, Pageable pageable);

    // ---- 키워드 모드: surType=2, 프로젝트코드/Kcode 검색 (연/월 필터 무시) ----
    @Query(value = LIST_SELECT + "where p.project_code ilike concat('%', :keyword, '%') " +
                   "or p.kcode ilike concat('%', :keyword, '%') order by p.project_code asc",
            countQuery = "select count(*) from project p where p.project_code ilike concat('%', :keyword, '%') " +
                         "or p.kcode ilike concat('%', :keyword, '%')",
            nativeQuery = true)
    Page<ProjectListRow> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ---- 상단 요약 박스: 연도 전체 매출/직접비 합계 ----
    @Query(value = "select coalesce(sum(p.revenue_amount), 0) from project p where p.year_code = :year", nativeQuery = true)
    BigDecimal sumRevenueByYear(@Param("year") String year);

    @Query(value = "select coalesce(sum(d.amount), 0) from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.year_code = :year", nativeQuery = true)
    BigDecimal sumDirectCostByYear(@Param("year") String year);

    // ---- 상단 요약 박스: 연도+월 매출/직접비 합계 ----
    @Query(value = "select coalesce(sum(p.revenue_amount), 0) from project p where p.year_code = :year and p.month_code = :month",
            nativeQuery = true)
    BigDecimal sumRevenueByYearMonth(@Param("year") String year, @Param("month") String month);

    @Query(value = "select coalesce(sum(d.amount), 0) from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.year_code = :year and p.month_code = :month", nativeQuery = true)
    BigDecimal sumDirectCostByYearMonth(@Param("year") String year, @Param("month") String month);

    // ---- 목록 하단 "합계" 행: 현재 검색 결과 전체에 대한 매출/직접비 합계 ----
    @Query(value = "select coalesce(sum(p.revenue_amount), 0) from project p where p.year_code = :year and p.month_code = :month",
            nativeQuery = true)
    BigDecimal sumRevenueForYearMonthFilter(@Param("year") String year, @Param("month") String month);

    @Query(value = "select coalesce(sum(d.amount), 0) from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.year_code = :year and p.month_code = :month", nativeQuery = true)
    BigDecimal sumDirectCostForYearMonthFilter(@Param("year") String year, @Param("month") String month);

    @Query(value = "select coalesce(sum(p.revenue_amount), 0) from project p " +
                   "where p.project_code ilike concat('%', :keyword, '%') or p.kcode ilike concat('%', :keyword, '%')",
            nativeQuery = true)
    BigDecimal sumRevenueForKeywordFilter(@Param("keyword") String keyword);

    @Query(value = "select coalesce(sum(d.amount), 0) from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.project_code ilike concat('%', :keyword, '%') or p.kcode ilike concat('%', :keyword, '%')",
            nativeQuery = true)
    BigDecimal sumDirectCostForKeywordFilter(@Param("keyword") String keyword);

    // ---- 연도 셀렉트박스 옵션: 데이터가 존재하는 연도 목록 ----
    @Query(value = "select distinct year_code from project order by year_code desc", nativeQuery = true)
    List<String> findDistinctYears();

    // ---- 통계 화면(/statistics): 선택 연도의 프로젝트 수 ----
    @Query(value = "select count(*) from project p where p.year_code = :year", nativeQuery = true)
    long countByYear(@Param("year") String year);

    // ---- 통계 화면: 선택 연도의 월별(01~12) 매출/직접비 합계 ----
    @Query(value = "select p.month_code as monthCode, coalesce(sum(p.revenue_amount), 0) as revenue, " +
                   "coalesce(sum(dc.item_total), 0) as directCost " +
                   "from project p " +
                   "left join (select project_id, sum(amount) as item_total from direct_cost_item group by project_id) dc " +
                   "  on dc.project_id = p.id " +
                   "where p.year_code = :year " +
                   "group by p.month_code order by p.month_code",
            nativeQuery = true)
    List<MonthlyStatRow> monthlyStatsForYear(@Param("year") String year);

    // ---- 통계 화면: 선택 연도의 클라이언트별 매출 합계 (상위 순) ----
    @Query(value = "select coalesce(p.client_name, '(미지정)') as label, sum(p.revenue_amount) as revenue " +
                   "from project p where p.year_code = :year " +
                   "group by p.client_name order by revenue desc",
            nativeQuery = true)
    List<GroupRevenueRow> revenueByClientForYear(@Param("year") String year);

    // ---- 통계 화면: 선택 연도의 프로젝트 종류별 매출 합계 ----
    @Query(value = "select coalesce(p.project_type, '(미지정)') as label, sum(p.revenue_amount) as revenue " +
                   "from project p where p.year_code = :year " +
                   "group by p.project_type order by revenue desc",
            nativeQuery = true)
    List<GroupRevenueRow> revenueByTypeForYear(@Param("year") String year);

    // ---- 통계 화면: 연도별 매출/직접비 추이 (전체 연도) ----
    @Query(value = "select p.year_code as yearCode, coalesce(sum(p.revenue_amount), 0) as revenue, " +
                   "coalesce(sum(dc.item_total), 0) as directCost " +
                   "from project p " +
                   "left join (select project_id, sum(amount) as item_total from direct_cost_item group by project_id) dc " +
                   "  on dc.project_id = p.id " +
                   "group by p.year_code order by p.year_code",
            nativeQuery = true)
    List<YearlyStatRow> revenueByYear();
}
