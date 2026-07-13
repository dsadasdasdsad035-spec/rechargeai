package com.wildai.support.repository;

import com.wildai.support.domain.SupportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    Page<SupportMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);
}
