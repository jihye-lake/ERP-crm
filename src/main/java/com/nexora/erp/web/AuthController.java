package com.nexora.erp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

/** 원본 Login.asp 화면에 대응하는 로그인 페이지 컨트롤러 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String logout,
                             @RequestParam(required = false) String expired,
                             Model model) {
        if (error != null) {
            model.addAttribute("message", "아이디 혹은 비밀번호를 확인해 주십시오.");
        } else if (logout != null) {
            model.addAttribute("message", "로그아웃 되었습니다.");
        } else if (expired != null) {
            model.addAttribute("message", "세션이 만료되었습니다. 다시 로그인해 주세요.");
        }
        return "login";
    }
}
