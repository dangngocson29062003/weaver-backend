package com.weaver.weaver_backend.dto.request.rabbitmq;

import com.weaver.weaver_backend.common.EmailType;
import com.weaver.weaver_backend.entity.User;

public record EmailRequest (User user, EmailType emailType) {
}
