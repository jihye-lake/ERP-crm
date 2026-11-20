package com.nexora.erp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
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

    /**
     * 프로젝트 번호(No). 1, 2, 3 ... 순서대로 DB(IDENTITY)가 매기는 영구 번호.
     * 중간 프로젝트가 삭제돼도 번호를 당기거나 재사용하지 않는다. (V5 마이그레이션)
     * 애플리케이션에서는 읽기만 하고, 저장 직후 DB가 만든 값을 다시 읽어온다.
     */
    @Generated(event = EventType.INSERT)
    @Column(name = "no", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long no;

    @Column(name = "project_code", nullable = false, length = 20)
    private String projectCode;

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

    /** 프로젝트코드(예: 26-DSH-BOT-01) 앞 2자리에서 DB가 만드는 generated column */
    @Column(name = "year_code", insertable = false, updatable = false, length = 4)
    private String yearCode;

    /**
     * 프로젝트 월("01"~"12"). 예전엔 코드(P26050001)에서 파생했지만
     * 새 코드 형식에는 월이 없어서 직접 저장한다. (V4 마이그레이션)
     */
    @Column(name = "month_code", nullable = false, length = 2)
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
