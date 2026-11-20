package com.nexora.erp.web;

import com.nexora.erp.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 역할별 접근 권한 + CSRF 확인.
 * 원본 ASP는 페이지마다 if 문으로 권한을 체크해서 빠진 곳이 있었음 → 여기서는 표로 한 번에 검증.
 * 계정은 시드 데이터(admin/editor/partner)를 그대로 사용 (@WithUserDetails).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityAccessTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ProjectRepository projectRepository;

    UUID anyProjectId;

    @BeforeEach
    void setUp() {
        anyProjectId = projectRepository.findAll().get(0).getId();
    }

    @Test
    void 로그인하지_않으면_로그인_페이지로_보낸다() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @ParameterizedTest(name = "{0} → {2}")
        @CsvSource({
                "admin,   admin1234,   /",
                "editor,  editor1234,  /",
                "partner, partner1234, /monitor",
        })
        void 역할별로_첫_화면이_다르다(String username, String password, String expectedUrl) throws Exception {
            mvc.perform(formLogin().user(username).password(password))
                    .andExpect(redirectedUrl(expectedUrl));
        }

        @Test
        void 비밀번호가_틀리면_실패() throws Exception {
            mvc.perform(formLogin().user("admin").password("wrong"))
                    .andExpect(redirectedUrl("/login?error=1"));
        }
    }

    @Nested
    @DisplayName("화면 접근 권한")
    class PageAccess {

        @ParameterizedTest(name = "[{0}] GET {1} → {2}")
        @CsvSource({
                "admin,   /,            200",
                "admin,   /monitor,     200",
                "admin,   /statistics,  200",
                "admin,   /audit-logs,  200",
                "editor,  /,            200",
                "editor,  /statistics,  200",
                "editor,  /audit-logs,  403",
                "partner, /,            403",
                "partner, /monitor,     200",
                "partner, /statistics,  403",
                "partner, /audit-logs,  403",
                "admin,   /projects/upload-template, 200",
                "editor,  /projects/upload-template, 403",
                "partner, /projects/upload-template, 403",
        })
        void 역할별_화면_접근(String username, String url, int expectedStatus) throws Exception {
            mvc.perform(get(url).with(user(username).roles(roleOf(username))))
                    .andExpect(status().is(expectedStatus));
        }

        @Test
        @WithUserDetails("partner")
        void 파트너사는_직접비를_볼_수_없다() throws Exception {
            mvc.perform(get("/direct-cost/{id}/view", anyProjectId)).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("editor")
        void 담당자는_직접비_입력은_가능() throws Exception {
            mvc.perform(get("/direct-cost/{id}", anyProjectId)).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("관리자 전용 AJAX (매출/삭제)")
    class AdminActions {

        @Test
        @WithUserDetails("editor")
        void 담당자는_매출을_수정할_수_없다() throws Exception {
            mvc.perform(post("/projects/{id}/revenue", anyProjectId).param("Cost", "1").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        void CSRF_토큰이_틀리면_관리자라도_거부하고_값도_안_바뀐다() throws Exception {
            BigDecimal before = projectRepository.findById(anyProjectId).orElseThrow().getRevenueAmount();

            mvc.perform(post("/projects/{id}/revenue", anyProjectId).param("Cost", "1").with(csrf().useInvalidToken()))
                    .andExpect(status().isForbidden());

            assertThat(projectRepository.findById(anyProjectId).orElseThrow().getRevenueAmount())
                    .isEqualByComparingTo(before);
        }

        @Test
        @WithUserDetails("admin")
        void CSRF_토큰_없이_세션도_없으면_로그인으로_돌려보낸다() throws Exception {
            // invalidSessionUrl 설정 때문에 403 대신 세션 만료 안내로 리다이렉트됨
            mvc.perform(post("/projects/{id}/revenue", anyProjectId).param("Cost", "1"))
                    .andExpect(redirectedUrl("/login?expired=1"));
        }

        @Test
        @WithUserDetails("admin")
        void 관리자는_CSRF_토큰과_함께_수정_가능() throws Exception {
            // 같은 금액으로 저장 (인메모리 DB를 다른 테스트와 공유하므로 데이터는 바꾸지 않음)
            String sameAmount = projectRepository.findById(anyProjectId).orElseThrow().getRevenueAmount().toPlainString();
            mvc.perform(post("/projects/{id}/revenue", anyProjectId).param("Cost", sameAmount).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Y"));
        }

        @Test
        @WithUserDetails("editor")
        void 담당자는_프로젝트를_추가할_수_있다() throws Exception {
            // 권한 통과 여부만 확인 (빈 제목이라 저장되지 않음 → 공유 인메모리 DB를 바꾸지 않음)
            mvc.perform(post("/projects").with(csrf()).contentType("application/json")
                            .content("{\"title\":\"\",\"revenueAmount\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("제목을 입력해 주세요."));
            mvc.perform(get("/projects/clients").param("q", "BOT")).andExpect(status().isOk());
            mvc.perform(get("/projects/managers").param("q", "김")).andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("partner")
        void 파트너사는_프로젝트를_추가하거나_검색할_수_없다() throws Exception {
            mvc.perform(post("/projects").with(csrf()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/projects/managers")).andExpect(status().isForbidden());
            mvc.perform(get("/projects/clients")).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        void 없는_프로젝트_삭제는_N() throws Exception {
            mvc.perform(post("/projects/{id}/delete", UUID.randomUUID()).with(csrf()))
                    .andExpect(content().string("N"));
        }
    }

    private static String roleOf(String username) {
        return switch (username) {
            case "admin" -> "ADMIN";
            case "editor" -> "EDITOR";
            default -> "VIEWER";
        };
    }
}
