package com.wildai.support.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_session")
public class SupportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_no", nullable = false, unique = true, length = 64)
    private String sessionNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String subject;

    @Column(nullable = false, length = 32)
    private String status = "OPEN";

    @Column(name = "last_message", length = 512)
    private String lastMessage;

    @Column(name = "unread_user_count", nullable = false)
    private int unreadUserCount;

    @Column(name = "unread_admin_count", nullable = false)
    private int unreadAdminCount;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionNo() { return sessionNo; }
    public void setSessionNo(String sessionNo) { this.sessionNo = sessionNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public int getUnreadUserCount() { return unreadUserCount; }
    public void setUnreadUserCount(int unreadUserCount) { this.unreadUserCount = unreadUserCount; }
    public int getUnreadAdminCount() { return unreadAdminCount; }
    public void setUnreadAdminCount(int unreadAdminCount) { this.unreadAdminCount = unreadAdminCount; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
