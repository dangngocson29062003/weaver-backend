package com.weaver.weaver_backend.service;


import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.TwoFAResponse;
import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.dto.response.user.UserDetailResponse;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserDetailResponse getMe(UUID userId);

    TwoFAResponse setupTwoFA(UUID userId);

    UserDetailResponse toggle2FA(UUID userId, String OTP);

    List<NotificationResponse>  getNotifications(UUID userId);

    void changePassword(UUID userId, PasswordRequest request);
}
