package com.nexora.erp.repository;

import com.nexora.erp.domain.DirectCostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DirectCostItemRepository extends JpaRepository<DirectCostItem, Long> {

    List<DirectCostItem> findByProjectId(UUID projectId);

    @Query("select coalesce(sum(d.amount), 0) from DirectCostItem d where d.project.id = :projectId")
    BigDecimal sumAmountByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("delete from DirectCostItem d where d.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);

    // ---- 통계 화면(/statistics): 선택 연도의 직접비 항목(카테고리)별 합계 ----
    @Query(value = "select d.category_code as categoryCode, sum(d.amount) as amount " +
                   "from direct_cost_item d join project p on p.id = d.project_id " +
                   "where p.year_code = :year " +
                   "group by d.category_code order by amount desc",
            nativeQuery = true)
    List<CategoryCostRow> costByCategoryForYear(@Param("year") String year);
}
