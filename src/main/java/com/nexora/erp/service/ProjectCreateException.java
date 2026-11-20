package com.nexora.erp.service;

/** 프로젝트 추가 입력값 오류. 메시지는 그대로 화면 alert 에 보여준다. */
public class ProjectCreateException extends RuntimeException {
    public ProjectCreateException(String message) {
        super(message);
    }
}
