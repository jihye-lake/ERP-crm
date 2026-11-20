package com.nexora.erp.repository;

import com.nexora.erp.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p.projectCode from Project p where p.projectCode in :codes")
    List<String> findExistingProjectCodes(@Param("codes") Collection<String> codes);

    /** 프로젝트 추가 시 다음 순번 계산용: "26-DSH-BOT-" 로 시작하는 코드 전부 */
    @Query("select p.projectCode from Project p where p.projectCode like concat(:prefix, '%')")
    List<String> findProjectCodesStartingWith(@Param("prefix") String prefix);

    /** 담당자 검색 모달: 지금까지 프로젝트에 등록된 담당자 이름 중 일부 일치 */
    @Query("select distinct p.manager from Project p " +
           "where p.manager is not null and p.manager <> '' and lower(p.manager) like lower(concat('%', :keyword, '%')) " +
           "order by p.manager")
    List<String> searchManagers(@Param("keyword") String keyword, Pageable pageable);

    String LIST_SELECT =
            // UUID를 그대로 반환하면 DB/드라이버에 따라 byte[]로 넘어와 프로젝션 변환이 실패하므로 문자열로 캐스팅
            "select cast(p.id as varchar) as id,p.no as no, p.project_code as projectCode, p.title as title, " +
            "p.project_type as projectType, p.manager as manager, p.client_name as clientName, " +
            "p.revenue_amount as revenueAmount, " +
            "coalesce((select sum(d.amount) from direct_cost_item d where d.project_id = p.id), 0) as directCostTotal " +
            "from project p ";

    // ---- 기본 모드: 연/월 필터 (searchType1/searchType2) ----
    @Query(value = LIST_SELECT + "where p.year_code = :year and p.month_code = :month order by p.project_code asc",
            countQuery = "select count(*) from project p where p.year_code = :year and p.month_code = :month",
            nativeQuery = true)
    Page<ProjectListRow> searchByYearMonth(@Param("year") String year, @Param("month") String month, Pageable pageable);

    // ---- 키워드 모드: surType=2, 프로젝트코드(부분 일치) / No(정확히 일치) 검색 (연/월 필터 무시) ----
    @Query(value = LIST_SELECT + "where p.project_code ilike concat('%', :keyword, '%') " +
                   "or cast(p.no as varchar) = :keyword order by p.project_code asc",
            countQuery = "select count(*) from project p where p.project_code ilike concat('%', :keyword, '%') " +
                         "or cast(p.no as varchar) = :keyword",
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
                   "where p.project_code ilike concat('%', :keyword, '%') or cast(p.no as varchar) = :keyword",
            nativeQuery = true)
    BigDecimal sumRevenueForKeywordFilter(@Param("keyword") String keyword);

    @Query(value = "select coalesce(sum(d.amount), 0) from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.project_code ilike concat('%', :keyword, '%') or cast(p.no as varchar) = :keyword",
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
