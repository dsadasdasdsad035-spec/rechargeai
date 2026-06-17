package com.wildai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wildai")
public class WildAiProperties {

    private Jwt jwt = new Jwt();
    private Aes aes = new Aes();
    private Order order = new Order();
    private Payment payment = new Payment();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Aes getAes() { return aes; }
    public void setAes(Aes aes) { this.aes = aes; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
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

    public static class Payment {
        private boolean mockEnabled = true;
        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    }
}
