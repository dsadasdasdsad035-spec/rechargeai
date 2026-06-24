package com.wildai.product.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "service_type_config")
public class ServiceTypeConfig {

    @Id
    @Column(name = "service_type", length = 64)
    private String serviceType;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "account_tutorial", columnDefinition = "TEXT")
    private String accountTutorial;

    @Column(name = "token_tutorial", columnDefinition = "TEXT")
    private String tokenTutorial;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAccountTutorial() { return accountTutorial; }
    public void setAccountTutorial(String accountTutorial) { this.accountTutorial = accountTutorial; }
    public String getTokenTutorial() { return tokenTutorial; }
    public void setTokenTutorial(String tokenTutorial) { this.tokenTutorial = tokenTutorial; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
