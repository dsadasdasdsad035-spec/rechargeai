package com.wildai.notify.repository;

import com.wildai.notify.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = 'PENDING' AND e.nextRunAt <= :now
            ORDER BY e.nextRunAt ASC
            """)
    List<OutboxEvent> findDuePending(Instant now);
}
