package com.wildai.product.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_service_product")
public class AiServiceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 64)
    private String productCode;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "service_type", nullable = false, length = 64)
    private String serviceType;

    @Column(name = "official_price", precision = 12, scale = 2)
    private BigDecimal officialPrice;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, length = 16)
    private String currency = "CNY";

    @Column(name = "period_days", nullable = false)
    private Integer periodDays;

    @Column(nullable = false, length = 32)
    private String status = "OFF_SHELF";

    @Column(name = "required_fields_json", nullable = false, columnDefinition = "JSON")
    private String requiredFieldsJson;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "refund_policy_text", columnDefinition = "TEXT")
    private String refundPolicyText;

    @Column(name = "compliance_notice", columnDefinition = "TEXT")
    private String complianceNotice;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public BigDecimal getOfficialPrice() { return officialPrice; }
    public void setOfficialPrice(BigDecimal officialPrice) { this.officialPrice = officialPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getPeriodDays() { return periodDays; }
    public void setPeriodDays(Integer periodDays) { this.periodDays = periodDays; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequiredFieldsJson() { return requiredFieldsJson; }
    public void setRequiredFieldsJson(String requiredFieldsJson) { this.requiredFieldsJson = requiredFieldsJson; }
    public Integer getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Integer estimatedHours) { this.estimatedHours = estimatedHours; }
    public String getRefundPolicyText() { return refundPolicyText; }
    public void setRefundPolicyText(String refundPolicyText) { this.refundPolicyText = refundPolicyText; }
    public String getComplianceNotice() { return complianceNotice; }
    public void setComplianceNotice(String complianceNotice) { this.complianceNotice = complianceNotice; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
