package com.weaver.weaver_backend.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import com.weaver.weaver_backend.common.CredentialStatus;
import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.TwoFAResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;
import com.weaver.weaver_backend.entity.Notification;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.exception.BadRequestException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.mapper.NotificationMapper;
import com.weaver.weaver_backend.mapper.UserMapper;
import com.weaver.weaver_backend.repository.NotificationRepository;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.service.other.GoogleAuthenticatorService;
import com.weaver.weaver_backend.service.IRedisTokenService;
import com.weaver.weaver_backend.service.IUserService;
import com.weaver.weaver_backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


    @Override
    public UserDetailResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    public TwoFAResponse setupTwoFA(UUID userId) {

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

        return new TwoFAResponse(qrUrl);
    }

    @Override
    public UserDetailResponse toggle2FA(UUID userId, String OTP) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String ggSecretKey = user.getTwoFaSecret();
        if (!ggSecretKey.isEmpty()) {
            boolean isVerified = ggAuthService.verifyCode(ggSecretKey, OTP);
            if (!isVerified) {
                throw new BadRequestException("OTP invalid");
            } else {
                user.setTwoFaEnabled(!user.getTwoFaEnabled());
                userRepository.save(user);
            }
        } else {
            throw new NotFoundException("User's google secret key not found");
        }
        return userMapper.toUserDetailResponse(user);
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

}
