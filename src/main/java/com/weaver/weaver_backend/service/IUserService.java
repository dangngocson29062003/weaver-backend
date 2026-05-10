package com.weaver.weaver_backend.service;


import com.weaver.weaver_backend.dto.request.user.PasswordRequest;
import com.weaver.weaver_backend.dto.response.user.*;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserDetailResponse getMe(UUID userId);

    TwoFASetupResponse setupTwoFA(UUID userId);

    TwoFAStatusResponse toggle2FA(UUID userId, String OTP);

    TwoFAStatusResponse disable2FAWithBackup(UUID userId, String rawBackupCode);

    List<NotificationResponse>  getNotifications(UUID userId);

    List<UserSessionResponse> getSessions(UUID userId, UUID currentSid);

    void changePassword(UUID userId, PasswordRequest request);

    void revokeSession(UUID sessionId);
}
