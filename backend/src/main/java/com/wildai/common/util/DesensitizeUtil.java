package com.wildai.common.util;

public final class DesensitizeUtil {

    private DesensitizeUtil() {}

    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "*" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    public static String account(String account) {
        if (account == null || account.length() <= 4) {
            return "****";
        }
        return account.substring(0, 2) + "****" + account.substring(account.length() - 2);
    }

    public static String tradeNo(String tradeNo) {
        if (tradeNo == null || tradeNo.length() <= 6) {
            return tradeNo;
        }
        return "****" + tradeNo.substring(tradeNo.length() - 6);
    }
}
