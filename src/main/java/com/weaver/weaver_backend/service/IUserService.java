package com.weaver.weaver_backend.service;


import com.nimbusds.jose.JOSEException;
import com.weaver.weaver_backend.dto.response.TwoFAResponse;
import com.weaver.weaver_backend.dto.response.auth.UserDetailResponse;

import java.text.ParseException;
import java.util.UUID;

public interface IUserService {
    UserDetailResponse getMe(UUID userId);

    TwoFAResponse setupTwoFA(UUID userId);
    UserDetailResponse toggle2FA(UUID userId, int OTP);
}
