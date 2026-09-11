package com.nexora.erp.web.dto;

import com.nexora.erp.domain.CostCategory;

import java.math.BigDecimal;

/**
 * 직접비 입력/조회 화면 한 행(row)의 뷰모델.
 * 원본에서 카테고리마다 개별 필드명(예: Meals_1, Meals_2, Meals_3, Meals_Total, Meals_note)으로
 * 흩어져 있던 것을 하나의 폼 객체 리스트로 정리했습니다.
 */
public class DirectCostItemForm {

    private CostCategory category;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal days;
    private BigDecimal amount = BigDecimal.ZERO;
    private String note;

    public DirectCostItemForm() {
    }

    public DirectCostItemForm(CostCategory category) {
        this.category = category;
    }

    public String getLabel() {
        return category.getLabel();
    }

    public String getHighlightClass() {
        return category.getHighlightClass();
    }

    public CostCategory getCategory() { return category; }
    public void setCategory(CostCategory category) { this.category = category; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
