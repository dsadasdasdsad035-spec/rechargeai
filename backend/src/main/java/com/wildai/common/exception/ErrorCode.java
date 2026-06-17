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
    ACCOUNT_DISABLED("USER_001", "账户已被禁用");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
