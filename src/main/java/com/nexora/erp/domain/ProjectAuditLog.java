package com.nexora.erp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프로젝트 변경 이력. 한 번 쌓이면 수정/삭제하지 않는다 (setter 없음).
 * project_id 는 일부러 FK/연관관계로 안 묶음 - 프로젝트가 지워져도 이력은 남아야 해서.
 */
@Entity
@Table(name = "project_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectAuditLog {

    private static final int MAX_VALUE_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "project_code", length = 20, updatable = false)
    private String projectCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditAction action;

    @Column(name = "old_value", length = MAX_VALUE_LENGTH, updatable = false)
    private String oldValue;

    @Column(name = "new_value", length = MAX_VALUE_LENGTH, updatable = false)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public ProjectAuditLog(UUID projectId, String projectCode, AuditAction action,
                           String oldValue, String newValue, String changedBy) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.action = action;
        this.oldValue = truncate(oldValue);
        this.newValue = truncate(newValue);
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH - 3) + "...";
    }
}
