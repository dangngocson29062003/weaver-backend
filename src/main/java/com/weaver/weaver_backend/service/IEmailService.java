package com.weaver.weaver_backend.service;

import com.weaver.weaver_backend.entity.User;

public interface IEmailService {
    void sendHtmlEmail(String to, String subject, String htmlContent);

    void sendWelcomeEmail(User user);

}
