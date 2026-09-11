package com.nexora.erp.service;

import com.nexora.erp.domain.AdminAccount;
import com.nexora.erp.repository.AdminAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 원본 ASP의 Session(PROJECTCODE & "_AdminId") / Session(PROJECTCODE & "_AdminName") 조회를 대체.
 * Spring Security의 인증 principal(username)로부터 표시용 이름/역할을 조회합니다.
 */
@Service
public class CurrentUserService {

    private final AdminAccountRepository adminAccountRepository;

    public CurrentUserService(AdminAccountRepository adminAccountRepository) {
        this.adminAccountRepository = adminAccountRepository;
    }

    public AdminAccount resolve(Authentication authentication) {
        return adminAccountRepository.findByUsernameAndEnabledTrue(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 계정을 찾을 수 없습니다: " + authentication.getName()));
    }
}
