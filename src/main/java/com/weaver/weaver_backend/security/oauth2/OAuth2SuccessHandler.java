package com.weaver.weaver_backend.security.oauth2;


import com.weaver.weaver_backend.common.AuthProvider;
import com.weaver.weaver_backend.dto.request.auth.LoginViaOAuthRequest;
import com.weaver.weaver_backend.dto.response.auth.LoginResponse;
import com.weaver.weaver_backend.service.IAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final IAuthService authService;
    @Value("${app.client-url}")
    private String clientUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken token =
                (OAuth2AuthenticationToken) authentication;
        String provider =
                token.getAuthorizedClientRegistrationId();
        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.get(
                        provider,
                        token.getPrincipal().getAttributes()
                );
        String email = userInfo.getEmail();
        String providerId = userInfo.getProviderId();
        AuthProvider providerEnum = AuthProvider.from(provider);
        LoginViaOAuthRequest oauthRequest = new LoginViaOAuthRequest(email, providerEnum, providerId);
        LoginResponse data = authService.loginViaOAuth(oauthRequest);
        Cookie refreshCookie = new Cookie("refresh_token", data.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(refreshCookie);
        String redirectUrl = clientUrl + "/oauth/success?accessToken=" + URLEncoder.encode(data.accessToken(), StandardCharsets.UTF_8);
        clearAuthenticationAttributes(
                request
        );
        getRedirectStrategy().sendRedirect(
                request,
                response,
                redirectUrl
        );
    }
}
