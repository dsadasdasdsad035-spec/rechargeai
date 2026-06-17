package com.wildai.payment.repository;

import com.wildai.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByPaymentNo(String paymentNo);
    Optional<PaymentTransaction> findByChannelAndThirdTradeNo(String channel, String thirdTradeNo);
    Optional<PaymentTransaction> findByOrderId(Long orderId);
}
