package com.nexora.erp.repository;

import com.nexora.erp.domain.ProjectAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectAuditLogRepository extends JpaRepository<ProjectAuditLog, Long> {

    Page<ProjectAuditLog> findAllByOrderByChangedAtDescIdDesc(Pageable pageable);

    Page<ProjectAuditLog> findByProjectCodeContainingIgnoreCaseOrderByChangedAtDescIdDesc(String projectCode, Pageable pageable);

    List<ProjectAuditLog> findByProjectIdOrderByChangedAtDescIdDesc(UUID projectId);
}
