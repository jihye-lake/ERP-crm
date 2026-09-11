package com.nexora.erp.web.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 직접비 입력 화면 저장 폼 (프로젝트 1건 + 카테고리별 27행) */
public class DirectCostForm {

    private UUID projectId;
    private List<DirectCostItemForm> items = new ArrayList<>();

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public List<DirectCostItemForm> getItems() { return items; }
    public void setItems(List<DirectCostItemForm> items) { this.items = items; }
}
