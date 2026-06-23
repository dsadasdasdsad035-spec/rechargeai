package com.wildai.refund.repository;

import com.wildai.refund.domain.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByRefundNo(String refundNo);

    List<RefundRequest> findByOrderIdAndStatusIn(Long orderId, List<String> statuses);
}
