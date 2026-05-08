package com.weaver.weaver_backend.dto.response.user;

import com.weaver.weaver_backend.common.CredentialStatus;
import com.weaver.weaver_backend.common.UserStatus;
import lombok.Builder;

@Builder
public record UserDetailResponse(
        String id,
        String email,
        String nickname,
        CredentialStatus credentialStatus,
        Boolean twoFaEnabled,
        String avatarUrl,
        UserStatus userStatus
) {
}
