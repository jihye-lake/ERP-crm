package com.nexora.erp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 원본 ASP의 [ERP_TProject] 테이블에 대응.
 * IDX는 원본에서 DB 기본값(uniqueidentifier)으로 자동 생성되던 것을
 * PostgreSQL gen_random_uuid()로 그대로 대체했습니다.
 * <p>
 * ID를 애플리케이션에서 직접 생성(UUID.randomUUID())해서 할당하기 때문에,
 * Spring Data JPA가 "이미 존재하는 엔티티"로 오인해 merge()를 시도하지 않도록
 * Persistable을 구현해 신규 저장 여부(isNew)를 명시적으로 판단합니다.
 */
@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project implements Persistable<UUID> {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_code", nullable = false, length = 20)
    private String projectCode;

    @Column(length = 20)
    private String kcode;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(length = 100)
    private String manager;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "client_manager", length = 100)
    private String clientManager;

    @Column(name = "revenue_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal revenueAmount = BigDecimal.ZERO;

    /**
     * 원본에서 "클라이언트명에 특정 협력사 문자열이 포함되어 있는지"로
     * Kcode(파트너 정산 코드) 입력란 노출 여부를 판단하던 로직을,
     * 클라이언트 이름 문자열 매칭 대신 명시적인 플래그로 대체했습니다.
     */
    @Column(name = "requires_partner_code", nullable = false)
    private boolean requiresPartnerCode = false;

    @Column(name = "year_code", insertable = false, updatable = false, length = 4)
    private String yearCode;

    @Column(name = "month_code", insertable = false, updatable = false, length = 2)
    private String monthCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isNew = true;

    public static Project newProject() {
        Project project = new Project();
        project.id = UUID.randomUUID();
        return project;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
