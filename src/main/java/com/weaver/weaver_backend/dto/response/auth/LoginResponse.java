package com.weaver.weaver_backend.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String accessToken,
                            String refreshToken,
                            String twoFAToken) {
}
