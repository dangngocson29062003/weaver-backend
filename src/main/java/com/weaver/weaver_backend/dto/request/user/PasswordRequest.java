package com.weaver.weaver_backend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record PasswordRequest(
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Length(min = 8, message = "New password must be at least 8 characters long")
        String newPassword) {
}
