package com.wildai.common.exception;

public enum ErrorCode {
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未登录或登录已过期"),
    FORBIDDEN("403", "无权限访问"),
    NOT_FOUND("404", "资源不存在"),
    CONFLICT("409", "业务冲突"),
    RATE_LIMITED("429", "请求过于频繁"),
    INTERNAL_ERROR("500", "系统内部错误"),
    ORDER_DUPLICATE("ORDER_001", "存在进行中的同产品订单"),
    ORDER_NOT_PAYABLE("ORDER_002", "订单不可支付"),
    PRODUCT_OFF_SHELF("PRODUCT_001", "产品已下架"),
    ACCOUNT_DISABLED("USER_001", "账户已被禁用"),
    REFUND_NOT_ALLOWED("REFUND_001", "当前订单不可申请退款"),
    REFUND_DUPLICATE("REFUND_002", "已有进行中的退款申请"),
    WITHDRAW_NOT_ALLOWED("WITHDRAW_001", "当前不可发起提现"),
    WITHDRAW_LIMIT("WITHDRAW_002", "超出提现限额或笔数上限"),
    WITHDRAW_INSUFFICIENT("WITHDRAW_003", "可提现余额不足"),
    PAYOUT_ACCOUNT_INVALID("PAYOUT_001", "收款账户不可用");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
