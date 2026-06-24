package com.wildai.finance.domain;

public final class WithdrawalStatus {

    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";
    public static final String PAYOUT_TIMEOUT = "PAYOUT_TIMEOUT";

    private WithdrawalStatus() {}
}
