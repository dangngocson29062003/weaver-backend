package com.weaver.weaver_backend.dto.response.user;

import java.util.List;

public record TwoFAStatusResponse(Boolean enabled,
                                  List<String> backupCodes) {
}
