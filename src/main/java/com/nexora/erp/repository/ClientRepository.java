package com.nexora.erp.repository;

import com.nexora.erp.domain.Client;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByName(String name);

    /** 이름 또는 약어 일부로 검색 (화면 클라이언트 검색 모달) */
    List<Client> findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseOrderByName(String name, String abbr, Pageable pageable);
}
