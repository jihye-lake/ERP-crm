package com.nexora.erp.service;

import com.nexora.erp.domain.AdminAccount;
import com.nexora.erp.repository.AdminAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 원본 Login_process.asp의 하드코딩된 4개 평문 계정 비교(EHLEE/3141 등) +
 * 특정 IP 자동 로그인 로직을 완전히 대체합니다.
 * 계정은 admin_account 테이블에서 조회하고 비밀번호는 BCrypt로 검증합니다.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminAccountRepository adminAccountRepository;

    public AdminUserDetailsService(AdminAccountRepository adminAccountRepository) {
        this.adminAccountRepository = adminAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccount account = adminAccountRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("아이디 혹은 비밀번호를 확인해 주십시오."));

        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(account.getRole().authority())
                .build();
    }
}
