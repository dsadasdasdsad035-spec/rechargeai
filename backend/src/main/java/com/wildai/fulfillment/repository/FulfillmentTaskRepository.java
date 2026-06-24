package com.wildai.fulfillment.repository;

import com.wildai.fulfillment.domain.FulfillmentTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, Long> {

    Optional<FulfillmentTask> findByOrderId(Long orderId);

    Optional<FulfillmentTask> findByTaskNo(String taskNo);

    @Query("""
            SELECT t FROM FulfillmentTask t, SubscriptionOrder o
            WHERE o.id = t.orderId
            AND (:status IS NULL OR t.status = :status)
            AND (:assigneeAdminId IS NULL OR t.assigneeAdminId = :assigneeAdminId)
            AND (:orderNo IS NULL OR o.orderNo = :orderNo)
            ORDER BY t.createdAt DESC
            """)
    Page<FulfillmentTask> searchAdminTasks(@Param("status") String status,
                                           @Param("assigneeAdminId") Long assigneeAdminId,
                                           @Param("orderNo") String orderNo,
                                           Pageable pageable);
}
