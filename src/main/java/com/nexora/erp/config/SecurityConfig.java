package com.nexora.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 원본의 IP 화이트리스트 자동 로그인 + 소스코드에 하드코딩된 평문 비밀번호 비교 로직을
 * Spring Security 표준 폼 로그인 + BCrypt 해시 검증으로 완전히 대체합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error=1")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=1")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/login?expired=1")
                .maximumSessions(1)
            )
            // 처음엔 원본 AJAX 호환 때문에 꺼뒀다가 다시 켬.
            // 폼은 th:action 으로 토큰 자동 삽입, AJAX는 layout.html 의 $.ajaxSetup 에서 헤더로 전송.
            // H2 콘솔(로컬 확인용)만 예외.
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // H2 콘솔이 iframe 사용

        return http.build();
    }
}
