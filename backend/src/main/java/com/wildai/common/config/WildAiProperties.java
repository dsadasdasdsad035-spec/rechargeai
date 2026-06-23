package com.wildai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wildai")
public class WildAiProperties {

    private Jwt jwt = new Jwt();
    private Aes aes = new Aes();
    private Order order = new Order();
    private Mail mail = new Mail();
    private Payment payment = new Payment();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Aes getAes() { return aes; }
    public void setAes(Aes aes) { this.aes = aes; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public static class Jwt {
        private String secret;
        private int userAccessExpirationMinutes = 30;
        private int userRefreshExpirationDays = 7;
        private int adminAccessExpirationMinutes = 480;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getUserAccessExpirationMinutes() { return userAccessExpirationMinutes; }
        public void setUserAccessExpirationMinutes(int v) { this.userAccessExpirationMinutes = v; }
        public int getUserRefreshExpirationDays() { return userRefreshExpirationDays; }
        public void setUserRefreshExpirationDays(int v) { this.userRefreshExpirationDays = v; }
        public int getAdminAccessExpirationMinutes() { return adminAccessExpirationMinutes; }
        public void setAdminAccessExpirationMinutes(int v) { this.adminAccessExpirationMinutes = v; }
    }

    public static class Aes {
        private String secretKey;
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }

    public static class Order {
        private int expireMinutes = 15;
        public int getExpireMinutes() { return expireMinutes; }
        public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "RechargeAi <noreply@rechargeai.local>";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class Payment {
        private boolean mockEnabled = true;
        private XunhuPay xunhupay = new XunhuPay();

        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public XunhuPay getXunhupay() { return xunhupay; }
        public void setXunhupay(XunhuPay xunhupay) { this.xunhupay = xunhupay; }

        public static class XunhuPay {
            private String appid;
            private String secret;
            private String gateway = "https://api.xunhupay.com";
            private String notifyUrl;
            private String returnUrl;
            private String callbackUrl;
            private int timeoutSeconds = 10;

            public String getAppid() { return appid; }
            public void setAppid(String appid) { this.appid = appid; }
            public String getSecret() { return secret; }
            public void setSecret(String secret) { this.secret = secret; }
            public String getGateway() { return gateway; }
            public void setGateway(String gateway) { this.gateway = gateway; }
            public String getNotifyUrl() { return notifyUrl; }
            public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
            public String getReturnUrl() { return returnUrl; }
            public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
            public String getCallbackUrl() { return callbackUrl; }
            public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        }
    }
}
