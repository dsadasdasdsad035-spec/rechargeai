package com.wildai.admin.repository;

import com.wildai.admin.domain.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    Page<AdminOperationLog> findByOperatorIdOrderByCreatedAtDesc(Long operatorId, Pageable pageable);

    @Query("""
            SELECT l FROM AdminOperationLog l WHERE
            (:operatorId IS NULL OR l.operatorId = :operatorId)
            AND (:operationType IS NULL OR l.operationType = :operationType)
            AND (:targetType IS NULL OR l.targetType = :targetType)
            AND (:targetId IS NULL OR l.targetId = :targetId)
            AND (:start IS NULL OR l.createdAt >= :start)
            AND (:end IS NULL OR l.createdAt <= :end)
            ORDER BY l.createdAt DESC
            """)
    Page<AdminOperationLog> search(@Param("operatorId") Long operatorId,
                                   @Param("operationType") String operationType,
                                   @Param("targetType") String targetType,
                                   @Param("targetId") String targetId,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end,
                                   Pageable pageable);
}
