package com.wildai.order.repository;

import com.wildai.order.domain.SubscriptionOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, Long> {

    Optional<SubscriptionOrder> findByOrderNo(String orderNo);

    Optional<SubscriptionOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    @Query("SELECT o FROM SubscriptionOrder o WHERE o.userId = :userId AND o.productId = :productId AND o.orderStatus IN ('WAIT_PAY','PAID','FULFILLING')")
    List<SubscriptionOrder> findActiveByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    Page<SubscriptionOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT o FROM SubscriptionOrder o WHERE o.userId = :userId
            AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
            AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
            AND (:start IS NULL OR o.createdAt >= :start)
            AND (:end IS NULL OR o.createdAt <= :end)
            ORDER BY o.createdAt DESC
            """)
    Page<SubscriptionOrder> searchUserOrders(@Param("userId") Long userId,
                                             @Param("orderStatus") String orderStatus,
                                             @Param("paymentStatus") String paymentStatus,
                                             @Param("start") Instant start,
                                             @Param("end") Instant end,
                                             Pageable pageable);

    @Query("""
            SELECT o FROM SubscriptionOrder o WHERE
            (:orderNo IS NULL OR o.orderNo = :orderNo)
            AND (:userId IS NULL OR o.userId = :userId)
            AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
            AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
            AND (:start IS NULL OR o.createdAt >= :start)
            AND (:end IS NULL OR o.createdAt <= :end)
            ORDER BY o.createdAt DESC
            """)
    Page<SubscriptionOrder> searchAdminOrders(@Param("orderNo") String orderNo,
                                              @Param("userId") Long userId,
                                              @Param("orderStatus") String orderStatus,
                                              @Param("paymentStatus") String paymentStatus,
                                              @Param("start") Instant start,
                                              @Param("end") Instant end,
                                              Pageable pageable);

    List<SubscriptionOrder> findByOrderStatusAndExpiredAtBefore(String orderStatus, Instant before);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM SubscriptionOrder o WHERE o.orderStatus = :orderStatus")
    java.math.BigDecimal sumAmountByOrderStatus(@Param("orderStatus") String orderStatus);

    long countByOrderStatus(String orderStatus);
}
