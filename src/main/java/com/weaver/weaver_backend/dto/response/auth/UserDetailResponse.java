package com.weaver.weaver_backend.dto.response.auth;

import com.weaver.weaver_backend.common.UserStatus;
import lombok.Builder;

@Builder
public record UserDetailResponse(
        String id,
        String email,
        String nickname,
        Boolean twoFaEnabled,
        String avatarUrl,
        UserStatus userStatus
) {
}
