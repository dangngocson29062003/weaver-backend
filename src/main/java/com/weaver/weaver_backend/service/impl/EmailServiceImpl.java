package com.weaver.weaver_backend.service.impl;

import com.weaver.weaver_backend.common.TokenType;
import com.weaver.weaver_backend.common.UserStatus;
import com.weaver.weaver_backend.dto.response.TokenResponse;
import com.weaver.weaver_backend.entity.RedisToken;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.exception.UnauthorizedException;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.IEmailService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class EmailServiceImpl implements IEmailService {

    private final JwtUtils jwtUtils;

    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;

    @Value("${app.client-url}")
    private String clientUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            ClassPathResource logoRes = new ClassPathResource("static/logo.png");
            if (logoRes.exists()) {
                helper.addInline("logoImage", logoRes);
            } else {
                log.warn("Logo file not found in static folder!");
            }
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Email sending failed");
        }
    }

    @Override
    public void sendWelcomeEmail(User user) {
        TokenResponse tokenResponse = jwtUtils.generateToken(user, TokenType.VERIFICATION_TOKEN);
        String verificationUrl = clientUrl + "/email/verify?token=" + tokenResponse.value();
        Context context = new Context();
        context.setVariable("username", user.getEmail());
        context.setVariable("verificationUrl", verificationUrl);
        String htmlContent = templateEngine.process("email-welcome", context);
        sendHtmlEmail(user.getEmail(), "Verify your email address", htmlContent);
    }
}
