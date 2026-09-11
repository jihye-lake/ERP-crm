package com.nexora.erp.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 원본 extend_session.asp: 세션 시간 연장 (세션에 접근하는 것만으로 최종 접근시각이 갱신됨) */
@RestController
public class SessionController {

    @PostMapping("/session/extend")
    public String extend(HttpServletRequest request) {
        request.getSession(); // 접근 자체로 세션 마지막 접근 시각이 갱신되어 타임아웃이 연장됨
        return "Session extended";
    }
}
