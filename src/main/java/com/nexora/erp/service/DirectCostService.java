package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.CostCategory;
import com.nexora.erp.domain.DirectCostItem;
import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.DirectCostItemRepository;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.web.dto.DirectCostItemForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 원본 DirectCost/index.asp(입력), DirectCost/Check.asp(조회), DirectCost/DirectCost_Pro.asp(저장)의
 * 27개 항목 직접비 로직에 대응.
 */
@Service
public class DirectCostService {

    private final DirectCostItemRepository directCostItemRepository;
    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    public DirectCostService(DirectCostItemRepository directCostItemRepository, ProjectRepository projectRepository,
                             AuditLogService auditLogService) {
        this.directCostItemRepository = directCostItemRepository;
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    /** 카테고리 27개 전체를 순서대로 반환 (저장된 값이 없으면 빈 값으로 채움) */
    public List<DirectCostItemForm> loadForProject(UUID projectId) {
        Map<CostCategory, DirectCostItem> existing = directCostItemRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(DirectCostItem::getCategory, item -> item, (a, b) -> a, () -> new EnumMap<>(CostCategory.class)));

        List<DirectCostItemForm> rows = new ArrayList<>();
        for (CostCategory category : CostCategory.orderedList()) {
            DirectCostItemForm form = new DirectCostItemForm(category);
            DirectCostItem item = existing.get(category);
            if (item != null) {
                form.setUnitPrice(item.getUnitPrice());
                form.setQuantity(item.getQuantity());
                form.setDays(item.getDays());
                form.setAmount(item.getAmount());
                form.setNote(item.getNote());
            }
            rows.add(form);
        }
        return rows;
    }

    public BigDecimal totalFor(UUID projectId) {
        BigDecimal total = directCostItemRepository.sumAmountByProjectId(projectId);
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * 원본 FuncSum()/FuncSum2() 자바스크립트의 "단가 x 투여량 x 일자" 합산 로직을 그대로 이식.
     * 세 값 중 0 또는 미입력인 값은 곱셈에서 제외되고(=1로 취급), 전부 비어있을 때만 0이 됩니다.
     */
    public static BigDecimal computeAmount(BigDecimal unitPrice, BigDecimal quantity, BigDecimal days) {
        BigDecimal product = BigDecimal.ONE;
        boolean allZero = true;
        for (BigDecimal value : List.of(nz(unitPrice), nz(quantity), nz(days))) {
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                allZero = false;
                product = product.multiply(value);
            }
        }
        return allZero ? BigDecimal.ZERO : product;
    }

    @Transactional
    public BigDecimal save(UUID projectId, List<DirectCostItemForm> items) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다: " + projectId));

        Map<CostCategory, DirectCostItem> existing = directCostItemRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(DirectCostItem::getCategory, item -> item, (a, b) -> a, () -> new EnumMap<>(CostCategory.class)));

        BigDecimal totalBefore = totalFor(projectId);
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (DirectCostItemForm form : items) {
            if (form.getCategory() == null) {
                continue; // 카테고리 값이 누락된 행은 무시 (정상 화면에서는 발생하지 않음)
            }

            BigDecimal amount = computeAmount(form.getUnitPrice(), form.getQuantity(), form.getDays());
            grandTotal = grandTotal.add(amount);

            boolean hasAnyValue = nz(form.getUnitPrice()).compareTo(BigDecimal.ZERO) != 0
                    || nz(form.getQuantity()).compareTo(BigDecimal.ZERO) != 0
                    || nz(form.getDays()).compareTo(BigDecimal.ZERO) != 0
                    || (form.getNote() != null && !form.getNote().isBlank());

            DirectCostItem item = existing.get(form.getCategory());
            if (item == null) {
                if (!hasAnyValue) {
                    continue; // 빈 행은 저장하지 않음
                }
                item = new DirectCostItem(project, form.getCategory());
            }

            item.setUnitPrice(form.getUnitPrice());
            item.setQuantity(form.getQuantity());
            item.setDays(form.getDays());
            item.setAmount(amount);
            item.setNote(form.getNote());
            directCostItemRepository.save(item);
        }

        // 항목별로 다 남기면 27줄씩 쌓여서 보기 힘들어서, 저장 단위로 합계 변화만 기록
        auditLogService.record(project, AuditAction.DIRECT_COST_UPDATE,
                ProjectService.plain(totalBefore), ProjectService.plain(totalFor(projectId)));

        return grandTotal;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
