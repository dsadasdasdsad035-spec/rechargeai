package com.wildai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "wildai")
public class WildAiProperties {

    private Jwt jwt = new Jwt();
    private Aes aes = new Aes();
    private Order order = new Order();
    private Mail mail = new Mail();
    private Payment payment = new Payment();
    private Finance finance = new Finance();
    private Tutorial tutorial = new Tutorial();
    private Article article = new Article();

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
    public Finance getFinance() { return finance; }
    public void setFinance(Finance finance) { this.finance = finance; }
    public Tutorial getTutorial() { return tutorial; }
    public void setTutorial(Tutorial tutorial) { this.tutorial = tutorial; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }

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
        private BigDecimal usdToCnyRate = new BigDecimal("7.250000");
        private XunhuPay xunhupay = new XunhuPay();

        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public BigDecimal getUsdToCnyRate() { return usdToCnyRate; }
        public void setUsdToCnyRate(BigDecimal usdToCnyRate) { this.usdToCnyRate = usdToCnyRate; }
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

    public static class Finance {
        private java.math.BigDecimal minWithdrawAmount = new java.math.BigDecimal("100");
        private java.math.BigDecimal maxDailyWithdrawAmount = new java.math.BigDecimal("50000");
        private int maxPendingWithdrawals = 3;
        private int payoutTimeoutDays = 7;

        public java.math.BigDecimal getMinWithdrawAmount() { return minWithdrawAmount; }
        public void setMinWithdrawAmount(java.math.BigDecimal v) { this.minWithdrawAmount = v; }
        public java.math.BigDecimal getMaxDailyWithdrawAmount() { return maxDailyWithdrawAmount; }
        public void setMaxDailyWithdrawAmount(java.math.BigDecimal v) { this.maxDailyWithdrawAmount = v; }
        public int getMaxPendingWithdrawals() { return maxPendingWithdrawals; }
        public void setMaxPendingWithdrawals(int v) { this.maxPendingWithdrawals = v; }
        public int getPayoutTimeoutDays() { return payoutTimeoutDays; }
        public void setPayoutTimeoutDays(int v) { this.payoutTimeoutDays = v; }
    }

    public static class Tutorial {
        private String uploadDir = "./uploads/tutorial";
        private long maxImageBytes = 5 * 1024 * 1024;
        private long maxVideoBytes = 50 * 1024 * 1024;

        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
        public long getMaxImageBytes() { return maxImageBytes; }
        public void setMaxImageBytes(long maxImageBytes) { this.maxImageBytes = maxImageBytes; }
        public long getMaxVideoBytes() { return maxVideoBytes; }
        public void setMaxVideoBytes(long maxVideoBytes) { this.maxVideoBytes = maxVideoBytes; }
    }

    public static class Article {
        private String uploadDir = "./uploads/article";
        private long maxImageBytes = 5 * 1024 * 1024;

        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
        public long getMaxImageBytes() { return maxImageBytes; }
        public void setMaxImageBytes(long maxImageBytes) { this.maxImageBytes = maxImageBytes; }
    }
}
