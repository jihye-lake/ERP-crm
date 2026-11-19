package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.Project;
import com.nexora.erp.domain.ProjectAuditLog;
import com.nexora.erp.repository.ProjectAuditLogRepository;
import com.nexora.erp.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class AuditLogIntegrationTest {

    @Autowired
    ProjectService projectService;
    @Autowired
    AuditLogService auditLogService;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    ProjectAuditLogRepository auditLogRepository;

    @Test
    void 매출을_바꾸면_이전값_이후값_작업자가_남는다() {
        Project project = anyProject();
        String before = ProjectService.plain(project.getRevenueAmount());

        projectService.updateRevenueAmount(project.getId(), new BigDecimal("12345000"));

        List<ProjectAuditLog> logs = auditLogRepository.findByProjectIdOrderByChangedAtDescIdDesc(project.getId());
        assertThat(logs).first().satisfies(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.REVENUE_UPDATE);
            assertThat(log.getOldValue()).isEqualTo(before);
            assertThat(log.getNewValue()).isEqualTo("12345000");
            assertThat(log.getChangedBy()).isEqualTo("admin");
        });
    }

    @Test
    void 같은_값으로_저장하면_이력을_남기지_않는다() {
        Project project = anyProject();

        projectService.updateKcode(project.getId(), project.getKcode());

        assertThat(auditLogRepository.findByProjectIdOrderByChangedAtDescIdDesc(project.getId())).isEmpty();
    }

    @Test
    void 프로젝트를_삭제해도_삭제_이력은_남는다() {
        Project project = anyProject();

        projectService.deleteProject(project.getId());

        assertThat(projectRepository.findById(project.getId())).isEmpty();
        assertThat(auditLogRepository.findByProjectIdOrderByChangedAtDescIdDesc(project.getId()))
                .extracting(ProjectAuditLog::getAction)
                .containsExactly(AuditAction.PROJECT_DELETE);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void 트랜잭션_밖에서_이력만_따로_남기는_건_막혀있다() {
        // 본 작업 없이 로그만 쌓이거나, 본 작업이 롤백됐는데 로그만 남는 걸 방지 (MANDATORY)
        Project project = anyProject();

        assertThatThrownBy(() -> auditLogService.record(project, AuditAction.KCODE_UPDATE, "a", "b"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Project anyProject() {
        return projectRepository.findAll().get(0);
    }
}
