package com.wildai.admin.repository;

import com.wildai.admin.domain.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    Page<AdminOperationLog> findByOperatorIdOrderByCreatedAtDesc(Long operatorId, Pageable pageable);
}
