package com.nexora.erp.web;

import com.nexora.erp.domain.Project;
import com.nexora.erp.repository.ClientRepository;
import com.nexora.erp.repository.ProjectRepository;
import com.nexora.erp.service.ProjectCreateException;
import com.nexora.erp.service.ProjectCreateService;
import com.nexora.erp.web.dto.ProjectCreateRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 목록의 "+" 행: 프로젝트 직접 추가 + 담당자/클라이언트 검색 모달용 조회 API.
 * 관리자(ADMIN)와 운영 담당자(EDITOR)만 사용 가능.
 */
@RestController
@RequestMapping("/projects")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
public class ProjectCreateController {

    private static final int SEARCH_LIMIT = 30;

    private final ProjectCreateService projectCreateService;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;

    public ProjectCreateController(ProjectCreateService projectCreateService, ProjectRepository projectRepository,
                                   ClientRepository clientRepository) {
        this.projectCreateService = projectCreateService;
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ProjectCreateRequest request) {
        try {
            Project project = projectCreateService.create(request);
            return ResponseEntity.ok(Map.of("ok", true, "projectCode", project.getProjectCode(), "no", project.getNo()));
        } catch (ProjectCreateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            // 같은 (연도, 종류, 클라이언트) 프로젝트를 다른 사람이 동시에 추가해 코드가 겹친 경우
            return ResponseEntity.status(409).body(Map.of("ok", false,
                    "message", "다른 사용자가 방금 같은 코드로 프로젝트를 추가했습니다. 다시 저장해 주세요."));
        }
    }

    @GetMapping("/managers")
    public List<String> managers(@RequestParam(defaultValue = "") String q) {
        return projectRepository.searchManagers(q.trim(), PageRequest.of(0, SEARCH_LIMIT));
    }

    @GetMapping("/clients")
    public List<Map<String, String>> clients(@RequestParam(defaultValue = "") String q) {
        String keyword = q.trim();
        return clientRepository
                .findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseOrderByName(keyword, keyword, PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(c -> Map.of("name", c.getName(), "abbr", c.getAbbr()))
                .toList();
    }
}
