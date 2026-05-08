package com.weaver.weaver_backend.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import com.weaver.weaver_backend.common.CredentialStatus;
import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.user.TwoFASetupResponse;
import com.weaver.weaver_backend.dto.response.user.TwoFAStatusResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.entity.Notification;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.entity.UserBackupCode;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.mapper.NotificationMapper;
import com.weaver.weaver_backend.mapper.UserMapper;
import com.weaver.weaver_backend.repository.NotificationRepository;
import com.weaver.weaver_backend.repository.UserBackupCodeRepository;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.other.GoogleAuthenticatorService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.service.IUserService;
import com.weaver.weaver_backend.util.JwtUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    private final GoogleAuthenticatorService ggAuthService;

    private final JwtUtils jwtUtils;

    private final IRedisTokenService redisTokenService;

    private final PasswordEncoder passwordEncoder;

    private final UserBackupCodeRepository userBackupCodeRepository;

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
        if(!user.getTwoFaEnabled()) {
            rawCodes = generateBackupCodes();
            List<UserBackupCode> backupCodes = rawCodes.stream()
                    .map(code -> UserBackupCode.builder()
                            .code(passwordEncoder.encode(code))
                            .user(user)
                            .build())
                    .toList();
            userBackupCodeRepository.saveAll(backupCodes);
            user.setTwoFaEnabled(true);
        }else {
            userBackupCodeRepository.deleteByUser(user);
            user.setTwoFaEnabled(false);
        }
        userRepository.save(user);

        return new TwoFAStatusResponse(user.getTwoFaEnabled(), rawCodes);
    }

    @Override
    public List<NotificationResponse> getNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toResponseList(notifications);
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
}
