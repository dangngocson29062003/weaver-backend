package com.weaver.weaver_backend.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

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


    @Override
    public UserDetailResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        System.out.println(user.getId());
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    public TwoFAResponse setupTwoFA(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String qrUrl = null;
        if(!user.getTwoFaEnabled()) {
            if(user.getTwoFaSecret() == null || user.getTwoFaSecret().isEmpty()) {
                GoogleAuthenticatorKey gAuthKey = ggAuthService.createCredentials();
                String secretKey = ggAuthService.getKey(gAuthKey);
                user.setTwoFaSecret(secretKey);
                userRepository.save(user);
                qrUrl = ggAuthService.getQRBarUrl(user.getEmail(), gAuthKey);
            }
        }else {
            qrUrl = ggAuthService.getQRBarUrl(user.getEmail(),
                    new GoogleAuthenticatorKey.Builder(user.getTwoFaSecret()).build());
        }
        return new TwoFAResponse(qrUrl);
    }

    @Override
    public UserDetailResponse toggle2FA(UUID userId, int OTP) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String ggSecretKey = user.getTwoFaSecret();
        if(!ggSecretKey.isEmpty()) {
            boolean isVerified = ggAuthService.verifyCode(ggSecretKey, OTP);
            if(!isVerified){
                throw new BadRequestException("OTP invalid");
            }else {
                user.setTwoFaEnabled(!user.getTwoFaEnabled());
                userRepository.save(user);
            }
        }else {
            throw new NotFoundException("User's google secret key not found");
        }
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    public List<NotificationResponse> getNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toResponseList(notifications);
    }

}
