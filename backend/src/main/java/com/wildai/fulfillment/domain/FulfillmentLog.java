package com.wildai.fulfillment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fulfillment_log")
public class FulfillmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "log_type", nullable = false, length = 32)
    private String logType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_visible", nullable = false)
    private boolean userVisible;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isUserVisible() { return userVisible; }
    public void setUserVisible(boolean userVisible) { this.userVisible = userVisible; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
