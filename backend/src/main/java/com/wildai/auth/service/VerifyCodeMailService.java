package com.wildai.auth.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerifyCodeMailService {

    private static final Logger log = LoggerFactory.getLogger(VerifyCodeMailService.class);

    private final JavaMailSender mailSender;
    private final WildAiProperties properties;

    public VerifyCodeMailService(JavaMailSender mailSender, WildAiProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendRegisterCode(String email, String code) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(email);
            message.setSubject("WildAI 注册验证码");
            message.setText("""
                    您好，

                    您正在注册 WildAI 订阅助手，验证码为：%s

                    验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略此邮件。

                    — WildAI 订阅助手
                    """.formatted(code));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送注册验证码邮件失败: {}", email, e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码邮件发送失败，请稍后重试");
        }
    }
}
