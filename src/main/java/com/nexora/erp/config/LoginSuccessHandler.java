package com.nexora.erp.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 원본 Login_process.asp의 "IF CInt(AdminId) &lt; 2 Then url = index.asp Else url = monitor.asp"
 * 라우팅 로직에 대응. 관리자/운영 담당자는 전체 대시보드로, 파트너사(VIEWER)는 모니터 화면으로 보냅니다.
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        boolean isViewer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_VIEWER"::equals);

        response.sendRedirect(request.getContextPath() + (isViewer ? "/monitor" : "/"));
    }
}
