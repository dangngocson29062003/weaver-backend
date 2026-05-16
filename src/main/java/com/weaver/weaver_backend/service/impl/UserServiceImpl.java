package com.weaver.weaver_backend.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.weaver.weaver_backend.common.CredentialStatus;
import com.weaver.weaver_backend.dto.request.rabbitmq.NotificationRequest;
import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.user.TwoFASetupResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFAStatusResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.dto.response.user.UserSessionResponse;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserBackupCode;
import com.weaver.weaver_backend.entity.UserSession;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.mapper.UserMapper;
import com.weaver.weaver_backend.mapper.UserSessionMapper;
import com.weaver.weaver_backend.mq.RabbitMQProducer;
import com.weaver.weaver_backend.repository.NotificationRepository;
import com.weaver.weaver_backend.repository.UserBackupCodeRepository;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.repository.UserSessionRepository;
import com.weaver.weaver_backend.service.IRedisSessionService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.service.IUserService;
import com.weaver.weaver_backend.service.other.GoogleAuthenticatorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.weaver.weaver_backend.common.NotificationType.TWO_FACTOR_DISABLED;
import static com.weaver.weaver_backend.common.NotificationType.TWO_FACTOR_ENABLED;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final UserSessionMapper userSessionMapper;

    private final RabbitMQProducer rabbitMQProducer;

    private final GoogleAuthenticatorService ggAuthService;

    private final PasswordEncoder passwordEncoder;

    private final UserBackupCodeRepository userBackupCodeRepository;

    private final UserSessionRepository userSessionRepository;

    private final IRedisSessionService redisSessionService;

    private final IRedisTokenService redisTokenService;

    private static final String CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom random = new SecureRandom();

    @Override
    public UserDetailResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    public TwoFASetupResponse setupTwoFA(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String secret;

        if (user.getTwoFaSecret() == null || user.getTwoFaSecret().isBlank()) {
            GoogleAuthenticatorKey gAuthKey = ggAuthService.createCredentials();
            secret = ggAuthService.getKey(gAuthKey);
            user.setTwoFaSecret(secret);
            userRepository.save(user);

        } else {
            secret = user.getTwoFaSecret();
        }

        String qrUrl = ggAuthService.getQRBarUrl(
                user.getEmail(),
                new GoogleAuthenticatorKey.Builder(secret).build()
        );

        return new TwoFASetupResponse(qrUrl);
    }

    @Override
    @Transactional
    public TwoFAStatusResponse toggle2FA(UUID userId, String OTP) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String ggSecretKey = user.getTwoFaSecret();
        if (ggSecretKey == null || ggSecretKey.isEmpty()) {
            throw new NotFoundException("User's google secret key not found");
        }
        boolean isVerified = ggAuthService.verifyCode(ggSecretKey, OTP);
        if (!isVerified) {
            throw new BadRequestException("OTP invalid");
        }
        List<String> rawCodes = new ArrayList<>();
        if (!user.getTwoFaEnabled()) {
            rawCodes = generateBackupCodes();
            List<UserBackupCode> backupCodes = rawCodes.stream()
                    .map(code -> UserBackupCode.builder()
                            .code(passwordEncoder.encode(code))
                            .user(user)
                            .build())
                    .toList();
            userBackupCodeRepository.saveAll(backupCodes);
            user.setTwoFaEnabled(true);
            NotificationRequest request = getRequestBasedOnVerifyStatus(userId, true);
            rabbitMQProducer.notify(request);
        }else {
            NotificationRequest request = getRequestBasedOnVerifyStatus(userId, false);
            rabbitMQProducer.notify(request);
            userBackupCodeRepository.deleteByUser(user);
            user.setTwoFaEnabled(false);
        }
        userRepository.save(user);

        return new TwoFAStatusResponse(user.getTwoFaEnabled(), rawCodes);
    }

    @Override
    @Transactional
    public TwoFAStatusResponse disable2FAWithBackup(UUID userId, String rawBackupCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getTwoFaEnabled()) {
            throw new BadRequestException("2FA is not enabled");
        }
        List<UserBackupCode> storedCodes = userBackupCodeRepository.findAllByUser(user);
        UserBackupCode validCode = storedCodes.stream()
                .filter(code -> passwordEncoder.matches(rawBackupCode, code.getCode()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid backup code"));
        userBackupCodeRepository.delete(validCode);
        NotificationRequest request = getRequestBasedOnVerifyStatus(userId, !user.getTwoFaEnabled());
        rabbitMQProducer.notify(request);
        user.setTwoFaEnabled(false);
        userBackupCodeRepository.deleteByUser(user);
        userRepository.save(user);
        return new TwoFAStatusResponse(false, new ArrayList<>());
    }

//    @Override
//    public List<NotificationResponse> getNotifications(UUID userId) {
//        List<Notification> notifications = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);
//        return notificationMapper.toResponseList(notifications);
//    }

    @Override
    public List<UserSessionResponse> getSessions(UUID userId, UUID currentSid) {
        return userSessionRepository.findAllByUserId(userId)
                .stream()
                .map(session -> userSessionMapper.toResponse(session, currentSid))
                .toList();
    }

    @Override
    public void changePassword(UUID userId, PasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getCredentialStatus() == CredentialStatus.PASSWORD_SET) {
            if (request.currentPassword() == null ||
                    request.currentPassword().isBlank()) {
                throw new BadRequestException("Current password is required");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BadRequestException("Invalid password");
            }
            if (passwordEncoder.matches(request.newPassword(), user.getPassword()
            )) {
                throw new BadRequestException(
                        "New password must be different from current password"
                );
            }
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCredentialStatus(CredentialStatus.PASSWORD_SET);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, userId).orElseThrow(() -> new NotFoundException("Session not found"));
        session.setIsRevoked(true);
        userSessionRepository.save(session);
        redisSessionService.revokeSession(sessionId.toString());
        redisTokenService.deleteAllBySessionId(sessionId);
    }

    @Override
    @Transactional
    public boolean toggleTrustDevice(UUID userId, UUID sessionId, String otp) {
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            throw new BadRequestException("Cannot toggle trust status for a revoked session");
        }
        if (Boolean.TRUE.equals(session.getIsTrusted())) {
            session.setIsTrusted(false);
        }
        else {
            if (otp == null || otp.isEmpty()) {
                throw new BadRequestException("OTP is required to trust this device");
            }

            String ggSecretKey = session.getUser().getTwoFaSecret();
            if (ggSecretKey == null || ggSecretKey.isEmpty()) {
                throw new BadRequestException("2FA not configured");
            }

            if (!ggAuthService.verifyCode(ggSecretKey, otp)) {
                throw new BadRequestException("OTP invalid");
            }
            session.setIsTrusted(true);
        }

        UserSession updatedSession = userSessionRepository.save(session);


        return updatedSession.getIsTrusted();
    }

    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            StringBuilder code =
                    new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String formatted = code.substring(0, 4) + "-" + code.substring(4);
            codes.add(formatted);
        }
        return codes;
    }

    private NotificationRequest getRequestBasedOnVerifyStatus(UUID userId,boolean isEnabled) {
        NotificationRequest notificationRequest = null;
        if(isEnabled) {
            notificationRequest = NotificationRequest.builder()
                    .type(TWO_FACTOR_ENABLED)
                    .title("Two-Factor Authentication Enabled")
                    .message("Your two-factor authentication has been enabled successfully.")
                    .userId(userId)
                    .build();
        }else {
            notificationRequest = NotificationRequest.builder()
                    .type(TWO_FACTOR_DISABLED)
                    .title("Two-Factor Authentication Disabled")
                    .message("Your two-factor authentication has been disabled successfully.")
                    .userId(userId)
                    .build();
        }

        return notificationRequest;
    }
}
