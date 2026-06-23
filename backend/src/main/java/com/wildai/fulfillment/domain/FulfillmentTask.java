package com.wildai.fulfillment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fulfillment_task")
public class FulfillmentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, unique = true, length = 64)
    private String taskNo;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "assignee_admin_id")
    private Long assigneeAdminId;

    @Column(name = "subscription_start")
    private Instant subscriptionStart;

    @Column(name = "subscription_end")
    private Instant subscriptionEnd;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAssigneeAdminId() { return assigneeAdminId; }
    public void setAssigneeAdminId(Long assigneeAdminId) { this.assigneeAdminId = assigneeAdminId; }
    public Instant getSubscriptionStart() { return subscriptionStart; }
    public void setSubscriptionStart(Instant subscriptionStart) { this.subscriptionStart = subscriptionStart; }
    public Instant getSubscriptionEnd() { return subscriptionEnd; }
    public void setSubscriptionEnd(Instant subscriptionEnd) { this.subscriptionEnd = subscriptionEnd; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
