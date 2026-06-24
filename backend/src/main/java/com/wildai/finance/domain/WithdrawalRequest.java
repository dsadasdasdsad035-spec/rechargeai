package com.wildai.finance.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "withdrawal_request")
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdrawal_no", nullable = false, unique = true, length = 64)
    private String withdrawalNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "payout_account_id", nullable = false)
    private Long payoutAccountId;

    @Column(name = "applicant_admin_id", nullable = false)
    private Long applicantAdminId;

    @Column(name = "approver_admin_id")
    private Long approverAdminId;

    @Column(name = "payout_confirmer_id")
    private Long payoutConfirmerId;

    @Column(name = "external_voucher_no", length = 128)
    private String externalVoucherNo;

    @Column(length = 512)
    private String remark;

    @Column(name = "reject_reason", length = 512)
    private String rejectReason;

    @Column(name = "variance_reason", length = 512)
    private String varianceReason;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWithdrawalNo() { return withdrawalNo; }
    public void setWithdrawalNo(String withdrawalNo) { this.withdrawalNo = withdrawalNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPayoutAccountId() { return payoutAccountId; }
    public void setPayoutAccountId(Long payoutAccountId) { this.payoutAccountId = payoutAccountId; }
    public Long getApplicantAdminId() { return applicantAdminId; }
    public void setApplicantAdminId(Long applicantAdminId) { this.applicantAdminId = applicantAdminId; }
    public Long getApproverAdminId() { return approverAdminId; }
    public void setApproverAdminId(Long approverAdminId) { this.approverAdminId = approverAdminId; }
    public Long getPayoutConfirmerId() { return payoutConfirmerId; }
    public void setPayoutConfirmerId(Long payoutConfirmerId) { this.payoutConfirmerId = payoutConfirmerId; }
    public String getExternalVoucherNo() { return externalVoucherNo; }
    public void setExternalVoucherNo(String externalVoucherNo) { this.externalVoucherNo = externalVoucherNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getVarianceReason() { return varianceReason; }
    public void setVarianceReason(String varianceReason) { this.varianceReason = varianceReason; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
