package com.wildai.support.repository;

import com.wildai.support.domain.SupportSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {
    Optional<SupportSession> findBySessionNo(String sessionNo);
    Page<SupportSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    Page<SupportSession> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
