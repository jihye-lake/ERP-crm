package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.Project;
import com.nexora.erp.domain.ProjectAuditLog;
import com.nexora.erp.repository.ProjectAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 매출/Kcode/직접비 등 정산에 영향 주는 변경을 기록.
 * 호출하는 쪽 트랜잭션에 같이 묶이게 MANDATORY로 둠 → 본 작업이 롤백되면 로그도 같이 롤백.
 */
@Service
public class AuditLogService {

    private static final String SYSTEM_USER = "system";

    private final ProjectAuditLogRepository auditLogRepository;

    public AuditLogService(ProjectAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Project project, AuditAction action, Object oldValue, Object newValue) {
        String before = oldValue == null ? null : String.valueOf(oldValue);
        String after = newValue == null ? null : String.valueOf(newValue);
        if (action != AuditAction.PROJECT_DELETE && Objects.equals(before, after)) {
            return; // 값이 그대로면 굳이 안 남김
        }
        auditLogRepository.save(new ProjectAuditLog(
                project.getId(), project.getProjectCode(), action, before, after, currentUsername()));
    }

    @Transactional(readOnly = true)
    public Page<ProjectAuditLog> search(String projectCode, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), size);
        if (projectCode == null || projectCode.isBlank()) {
            return auditLogRepository.findAllByOrderByChangedAtDescIdDesc(pageable);
        }
        return auditLogRepository.findByProjectCodeContainingIgnoreCaseOrderByChangedAtDescIdDesc(projectCode.trim(), pageable);
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null || auth.getName() == null) ? SYSTEM_USER : auth.getName();
    }
}
