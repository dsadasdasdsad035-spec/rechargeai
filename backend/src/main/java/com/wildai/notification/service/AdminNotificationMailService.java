package com.wildai.notification.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSessionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AdminNotificationMailService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationMailService.class);

    private final JavaMailSender mailSender;
    private final WildAiProperties properties;

    public AdminNotificationMailService(JavaMailSender mailSender, WildAiProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendSupportOfflineNotification(String to, SupportSessionDto session, SupportMessageDto message) {
        String subject = "RechargeAi 在线客服离线提醒：" + session.sessionNo();
        String text = """
                后台在线客服当前无人在线，用户发来新的咨询消息。

                会话编号：%s
                咨询主题：%s
                用户 ID：%s
                消息内容：%s
                发送时间：%s

                请尽快登录管理后台处理。
                """.formatted(
                session.sessionNo(),
                session.subject(),
                session.userId(),
                message.content(),
                message.createdAt()
        );
        send(to, subject, text);
    }

    public void sendOrderPaidNotification(String to, SubscriptionOrder order, PaymentTransaction payment) {
        String subject = "RechargeAi 订单支付完成：" + order.getOrderNo();
        String text = """
                用户订单已支付完成。

                订单编号：%s
                用户 ID：%s
                商品 ID：%s
                订单金额：%s %s
                实际支付：%s %s
                支付汇率：%s
                支付渠道：%s
                支付单号：%s
                三方交易号：%s
                支付时间：%s

                请登录管理后台查看订单和履约任务。
                """.formatted(
                order.getOrderNo(),
                order.getUserId(),
                order.getProductId(),
                money(order.getAmount()),
                valueOrDash(order.getCurrency()),
                money(payment.getAmountDecimal()),
                valueOrDash(payment.getCurrency()),
                payment.getExchangeRateDecimal() != null ? payment.getExchangeRateDecimal().toPlainString() : "-",
                valueOrDash(payment.getChannel()),
                valueOrDash(payment.getPaymentNo()),
                valueOrDash(payment.getThirdTradeNo()),
                payment.getPaidAt()
        );
        send(to, subject, text);
    }

    private void send(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            log.warn("管理员通知邮箱未配置，跳过发送邮件：{}", subject);
            return;
        }
        if (!properties.getMail().isEnabled()) {
            log.info("邮件通知未启用，跳过发送邮件：{}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送管理员通知邮件失败：{}", to, e);
        }
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = value.toString();
        return text.isBlank() ? "-" : text;
    }
}
