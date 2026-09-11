package com.nexora.erp.web;

import jakarta.servlet.http.HttpSession;

/**
 * 원본의 Session("StartedAt") 기반 남은 시간 계산(index.asp 상단)을 대체.
 * 서블릿 컨테이너의 세션 최종 접근 시각 + maxInactiveInterval을 그대로 활용합니다.
 */
public final class SessionTimeUtil {

    private SessionTimeUtil() {
    }

    public static long remainingSeconds(HttpSession session) {
        long lastAccessed = session.getLastAccessedTime();
        long maxInactiveMillis = (long) session.getMaxInactiveInterval() * 1000;
        long elapsed = System.currentTimeMillis() - lastAccessed;
        long remainingMillis = maxInactiveMillis - elapsed;
        return Math.max(0, remainingMillis / 1000);
    }
}
