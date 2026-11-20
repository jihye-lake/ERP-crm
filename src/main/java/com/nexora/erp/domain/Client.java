package com.nexora.erp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클라이언트 마스터 (V6). 프로젝트코드의 클라이언트 약어(26-DSH-<b>BOT</b>-01)를 만들 때 사용.
 * project.client_name 은 지금처럼 이름 문자열로 저장하고, 여기와는 이름으로 매칭한다.
 */
@Entity
@Table(name = "client")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200, unique = true)
    private String name;

    @Column(nullable = false, length = 3, unique = true)
    private String abbr;
}
