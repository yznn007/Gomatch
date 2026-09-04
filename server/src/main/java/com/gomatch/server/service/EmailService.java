package com.gomatch.server.service;

import com.gomatch.database.config.RuntimeConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final EmailService INSTANCE = new EmailService();

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean configured;

    private EmailService() {
        Properties runtime = RuntimeConfig.loadRuntimeProperties("database.properties");
        host = getConfig(runtime, "mail.host", "MAIL_HOST", "smtp.qq.com");
        port = Integer.parseInt(getConfig(runtime, "mail.port", "MAIL_PORT", "587"));
        username = getConfig(runtime, "mail.user", "MAIL_USER", "");
        password = getConfig(runtime, "mail.password", "MAIL_PASSWORD", "");
        from = getConfig(runtime, "mail.from", "MAIL_FROM", username);
        configured = !username.isEmpty() && !password.isEmpty();
        if (configured) {
            log.info("EmailService configured: host={}, port={}, from={}", host, port, from);
        } else {
            log.warn("EmailService NOT configured. Set MAIL_USER and MAIL_PASSWORD env vars to enable real email sending.");
        }
    }

    public static EmailService getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean sendVerificationCode(String toEmail, String code) {
        if (!configured) {
            log.info("[DEV MODE] Verification code for {}: {}", toEmail, code);
            return true;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("[Gomatch] 密码重置验证码 / Password Reset Code");
            message.setText(buildEmailContent(code));

            Transport.send(message);
            log.info("Verification code sent to {}", toEmail);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send verification code to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String buildEmailContent(String code) {
        long ttl = com.gomatch.server.handler.VerificationCodeManager.CODE_TTL_SECONDS;
        return """
                [Gomatch] 密码重置验证码

                您的验证码是：%s
                有效期 %d 秒，请勿泄露给他人。

                ---
                [Gomatch] Password Reset Code

                Your verification code is: %s
                Valid for %d seconds. Do not share with others.
                """.formatted(code, ttl, code, ttl);
    }

    private static String getConfig(Properties runtime, String property, String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) return env;
        String prop = System.getProperty(key);
        if (prop != null && !prop.isEmpty()) return prop;
        String dotEnv = runtime.getProperty(property);
        if (dotEnv != null && !dotEnv.isEmpty()) return dotEnv;
        return defaultValue;
    }
}
