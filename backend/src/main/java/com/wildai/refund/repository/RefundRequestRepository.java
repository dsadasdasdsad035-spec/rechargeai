package com.wildai.refund.repository;

import com.wildai.refund.domain.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByRefundNo(String refundNo);

    List<RefundRequest> findByOrderIdAndStatusIn(Long orderId, List<String> statuses);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<String> statuses);

    Optional<RefundRequest> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("""
            SELECT r FROM RefundRequest r, SubscriptionOrder o
            WHERE o.id = r.orderId
            AND (:status IS NULL OR r.status = :status)
            AND (:orderNo IS NULL OR o.orderNo = :orderNo)
            AND (:refundNo IS NULL OR r.refundNo = :refundNo)
            ORDER BY r.createdAt DESC
            """)
    Page<RefundRequest> searchAdminRefunds(@Param("status") String status,
                                           @Param("orderNo") String orderNo,
                                           @Param("refundNo") String refundNo,
                                           Pageable pageable);
}
