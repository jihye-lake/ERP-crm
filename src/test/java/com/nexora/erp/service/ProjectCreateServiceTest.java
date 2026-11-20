package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.CostCategory;
import com.nexora.erp.domain.DirectCostItem;
import com.nexora.erp.domain.Project;
import com.nexora.erp.domain.ProjectAuditLog;
import com.nexora.erp.repository.ClientRepository;
import com.nexora.erp.repository.DirectCostItemRepository;
import com.nexora.erp.repository.ProjectAuditLogRepository;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.web.dto.ProjectCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 화면 "+" 행으로 프로젝트 추가 (h2, 각 테스트 롤백) */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ProjectCreateServiceTest {

    @Autowired
    ProjectCreateService service;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    DirectCostItemRepository directCostItemRepository;
    @Autowired
    ProjectAuditLogRepository auditLogRepository;

    @Test
    void 코드는_같은_연도_종류_클라이언트의_다음_순번으로_자동생성되고_직접비와_이력이_남는다() {
        // 시드에 26-DSH-BOT-01 ~ 04 가 있음
        Project p = service.create(request("신규 대시보드", "대시보드구축", "김도윤", "블루오션테크", 12_000_000, 3_000_000, "2026", "3"));

        assertThat(p.getProjectCode()).isEqualTo("26-DSH-BOT-05");
        assertThat(p.getMonthCode()).isEqualTo("03");
        assertThat(p.getNo()).isNotNull();
        assertThat(p.getManager()).isEqualTo("김도윤");

        List<DirectCostItem> costs = directCostItemRepository.findByProjectId(p.getId());
        assertThat(costs).singleElement().satisfies(c -> {
            assertThat(c.getCategory()).isEqualTo(CostCategory.ETC_1);
            assertThat(c.getAmount()).isEqualByComparingTo("3000000");
        });

        assertThat(auditLogRepository.findByProjectIdOrderByChangedAtDescIdDesc(p.getId()))
                .extracting(ProjectAuditLog::getAction).containsExactly(AuditAction.PROJECT_CREATE);
    }

    @Test
    void 처음_나오는_조합이면_01번이고_직접비_0이면_직접비_항목을_만들지_않는다() {
        Project p = service.create(request("컨설팅", "컨설팅", "", "블루오션테크", 1_000, 0, "2027", "12"));

        assertThat(p.getProjectCode()).isEqualTo("27-CON-BOT-01");
        assertThat(p.getManager()).isNull();
        assertThat(directCostItemRepository.findByProjectId(p.getId())).isEmpty();
    }

    @Test
    void 중간_순번이_비어도_가장_큰_순번_다음으로_만든다() {
        Project second = projectRepository.findAll().stream()
                .filter(x -> x.getProjectCode().equals("26-DSH-BOT-02")).findFirst().orElseThrow();
        directCostItemRepository.deleteByProjectId(second.getId());
        projectRepository.delete(second);
        projectRepository.flush();

        assertThat(service.create(request("a", "대시보드구축", "", "블루오션테크", 1, 0, "2026", "1")).getProjectCode())
                .isEqualTo("26-DSH-BOT-05");
    }

    @Test
    void 입력값_오류() {
        assertThatThrownBy(() -> service.create(request(" ", "컨설팅", "", "블루오션테크", 1, 0, "2026", "1")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("제목");
        assertThatThrownBy(() -> service.create(request("t", "기타", "", "블루오션테크", 1, 0, "2026", "1")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("종류");
        assertThatThrownBy(() -> service.create(request("t", "컨설팅", "", "없는회사", 1, 0, "2026", "1")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("클라이언트");
        assertThatThrownBy(() -> service.create(new ProjectCreateRequest("t", "컨설팅", "", "블루오션테크", null, null, "2026", "1")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("매출");
        assertThatThrownBy(() -> service.create(request("t", "컨설팅", "", "블루오션테크", 1, -5, "2026", "1")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("0 이상");
        assertThatThrownBy(() -> service.create(request("t", "컨설팅", "", "블루오션테크", 1, 0, "2026", "13")))
                .isInstanceOf(ProjectCreateException.class).hasMessageContaining("연/월");
    }

    @Test
    void 담당자_클라이언트_검색() {
        assertThat(projectRepository.searchManagers("도", PageRequest.of(0, 30))).contains("김도윤").doesNotHaveDuplicates();
        assertThat(clientRepository.findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseOrderByName("bot", "bot", PageRequest.of(0, 30)))
                .extracting(c -> c.getName()).containsExactly("블루오션테크");
    }

    private static ProjectCreateRequest request(String title, String type, String manager, String client,
                                                long revenue, long cost, String year, String month) {
        return new ProjectCreateRequest(title, type, manager, client,
                BigDecimal.valueOf(revenue), BigDecimal.valueOf(cost), year, month);
    }
}
