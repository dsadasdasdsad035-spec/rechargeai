package com.wildai.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, unique = true, length = 64)
    private String paymentNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "third_trade_no", length = 128)
    private String thirdTradeNo;

    @Column(name = "callback_verified", nullable = false)
    private Boolean callbackVerified = false;

    @Column(name = "callback_raw_redacted", columnDefinition = "TEXT")
    private String callbackRawRedacted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getAmount() { return amount != null ? amount.toPlainString() : null; }
    public BigDecimal getAmountDecimal() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getThirdTradeNo() { return thirdTradeNo; }
    public void setThirdTradeNo(String thirdTradeNo) { this.thirdTradeNo = thirdTradeNo; }
    public Boolean getCallbackVerified() { return callbackVerified; }
    public void setCallbackVerified(Boolean callbackVerified) { this.callbackVerified = callbackVerified; }
    public String getCallbackRawRedacted() { return callbackRawRedacted; }
    public void setCallbackRawRedacted(String callbackRawRedacted) { this.callbackRawRedacted = callbackRawRedacted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
