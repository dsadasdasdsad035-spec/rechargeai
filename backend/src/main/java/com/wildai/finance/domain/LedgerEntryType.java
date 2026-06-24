package com.wildai.finance.domain;

public final class LedgerEntryType {

    public static final String SETTLE = "SETTLE";
    public static final String REFUND = "REFUND";
    public static final String WITHDRAW_FREEZE = "WITHDRAW_FREEZE";
    public static final String WITHDRAW_RELEASE = "WITHDRAW_RELEASE";
    public static final String WITHDRAW_COMPLETE = "WITHDRAW_COMPLETE";

    private LedgerEntryType() {}
}
