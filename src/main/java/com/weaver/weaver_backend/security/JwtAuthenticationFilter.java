package com.weaver.weaver_backend.security;

import com.weaver.weaver_backend.common.TokenType;
import com.weaver.weaver_backend.common.UserStatus;
import com.weaver.weaver_backend.dto.response.auth.AuthUserResponse;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.exception.ForbiddenException;
import com.weaver.weaver_backend.exception.NotFoundException;
import com.weaver.weaver_backend.exception.UnauthorizedException;
import com.weaver.weaver_backend.repository.UserRepository;
import com.weaver.weaver_backend.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = parseJwt(request);
            if (token != null) {
                Claims claims = jwtUtils.extractClaims(token);
                String jwtId = jwtUtils.getJwtId(claims);
                if (!jwtUtils.isTokenBlacklisted(jwtId)) {
                    TokenType tokenType = jwtUtils.getType(claims);
                    if (tokenType != TokenType.ACCESS_TOKEN) {
                        throw new UnauthorizedException("Invalid access token");
                    }
                    UUID userId = jwtUtils.getUserId(claims);
                    String email = jwtUtils.getEmail(claims);
                    Boolean verified = jwtUtils.getVerified(claims);
                    if (!verified) {
                        throw new UnauthorizedException("Please verify your email");
                    }
                    UserStatus status = jwtUtils.getStatus(claims);
                    if (status == UserStatus.INACTIVE) {
                        throw new UnauthorizedException("User locked");
                    }
                    AuthUserResponse authUser = new AuthUserResponse(userId, email);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(authUser, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            logger.error("Cannot set user authentication: {}", ex);
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
