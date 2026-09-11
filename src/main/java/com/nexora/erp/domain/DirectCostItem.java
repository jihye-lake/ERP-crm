package com.nexora.erp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 원본 [ERP_direct_cost] 테이블의 "카테고리별 단가/투여량/일자/금액/비고" 반복 컬럼(총 135개)을
 * 프로젝트당 최대 27행(CostCategory 하나당 한 행)으로 정규화한 자식 엔티티.
 */
@Entity
@Table(name = "direct_cost_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "category_code"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectCostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 30)
    private CostCategory category;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 15, scale = 2)
    private BigDecimal days;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    public DirectCostItem(Project project, CostCategory category) {
        this.project = project;
        this.category = category;
    }
}
