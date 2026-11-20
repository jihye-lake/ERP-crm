package com.nexora.erp.service;

import com.nexora.erp.domain.AuditAction;
import com.nexora.erp.domain.Client;
import com.nexora.erp.domain.CostCategory;
import com.nexora.erp.domain.DirectCostItem;
import com.nexora.erp.domain.Project;
import com.nexora.erp.domain.ProjectType;
import com.nexora.erp.repository.ClientRepository;
import com.nexora.erp.repository.DirectCostItemRepository;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.web.dto.ProjectCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 화면에서 프로젝트 한 건 직접 추가.
 * <p>
 * 프로젝트코드는 "연도2-종류약어-클라이언트약어-순번2"(예: 26-DSH-BOT-05)로 자동 생성한다.
 * 순번은 같은 (연도, 종류, 클라이언트) 안의 가장 큰 순번 + 1.
 * 두 사람이 동시에 같은 조합을 추가하면 DB UNIQUE(uq_project_code)에 걸려 한쪽은 실패하고, 다시 시도하면 된다.
 * <p>
 * 직접비는 27개 항목 중 "9-1. 기타1"에 금액으로 넣어 둔다. 나중에 직접비 입력 화면에서 항목별로 나눠 입력하면 됨.
 */
@Service
public class ProjectCreateService {

    static final CostCategory INITIAL_COST_CATEGORY = CostCategory.ETC_1;
    static final String INITIAL_COST_NOTE = "프로젝트 추가 시 입력한 직접비";

    private static final Pattern YEAR = Pattern.compile("^20[0-9]{2}$");
    private static final Pattern MONTH = Pattern.compile("^(0?[1-9]|1[0-2])$");
    private static final Pattern SEQ_SUFFIX = Pattern.compile("-([0-9]{2})$");

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final DirectCostItemRepository directCostItemRepository;
    private final AuditLogService auditLogService;

    public ProjectCreateService(ProjectRepository projectRepository, ClientRepository clientRepository,
                                DirectCostItemRepository directCostItemRepository, AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.directCostItemRepository = directCostItemRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Project create(ProjectCreateRequest req) {
        String title = trim(req.title());
        if (title.isEmpty()) {
            throw new ProjectCreateException("제목을 입력해 주세요.");
        }
        if (title.length() > 500) {
            throw new ProjectCreateException("제목은 500자까지 입력할 수 있습니다.");
        }
        ProjectType type = ProjectType.fromLabel(trim(req.projectType()))
                .orElseThrow(() -> new ProjectCreateException("종류를 선택해 주세요."));
        Client client = clientRepository.findByName(trim(req.clientName()))
                .orElseThrow(() -> new ProjectCreateException("클라이언트를 검색해서 선택해 주세요."));
        String manager = trim(req.manager());
        if (manager.length() > 100) {
            throw new ProjectCreateException("담당자 이름이 너무 깁니다.");
        }
        BigDecimal revenue = req.revenueAmount();
        if (revenue == null) {
            throw new ProjectCreateException("매출 금액을 입력해 주세요.");
        }
        BigDecimal directCost = req.directCost() == null ? BigDecimal.ZERO : req.directCost();
        if (revenue.signum() < 0 || directCost.signum() < 0) {
            throw new ProjectCreateException("금액은 0 이상이어야 합니다.");
        }
        String year = trim(req.year());
        String month = trim(req.month());
        if (!YEAR.matcher(year).matches() || !MONTH.matcher(month).matches()) {
            throw new ProjectCreateException("연/월 값이 올바르지 않습니다.");
        }

        Project project = Project.newProject();
        project.setProjectCode(nextProjectCode(year.substring(2), type, client));
        project.setTitle(title);
        project.setProjectType(type.getLabel());
        project.setManager(manager.isEmpty() ? null : manager);
        project.setClientName(client.getName());
        project.setRevenueAmount(revenue);
        project.setMonthCode(String.format("%02d", Integer.parseInt(month)));
        projectRepository.saveAndFlush(project); // 코드 중복(동시 등록)을 여기서 바로 확인

        if (directCost.signum() > 0) {
            DirectCostItem item = new DirectCostItem(project, INITIAL_COST_CATEGORY);
            item.setUnitPrice(directCost);
            item.setAmount(DirectCostService.computeAmount(directCost, null, null));
            item.setNote(INITIAL_COST_NOTE);
            directCostItemRepository.save(item);
        }

        auditLogService.record(project, AuditAction.PROJECT_CREATE, null,
                title + " / 매출 " + ProjectService.plain(revenue) + " / 직접비 " + ProjectService.plain(directCost));
        return project;
    }

    /** "26-DSH-BOT-" 중 가장 큰 순번 + 1 */
    String nextProjectCode(String yy, ProjectType type, Client client) {
        String prefix = yy + "-" + type.getAbbr() + "-" + client.getAbbr() + "-";
        int max = 0;
        for (String code : projectRepository.findProjectCodesStartingWith(prefix)) {
            Matcher m = SEQ_SUFFIX.matcher(code);
            if (m.find()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        if (max >= 99) {
            throw new ProjectCreateException(prefix + " 순번이 99를 넘어 더 이상 코드를 만들 수 없습니다.");
        }
        return prefix + String.format("%02d", max + 1);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
