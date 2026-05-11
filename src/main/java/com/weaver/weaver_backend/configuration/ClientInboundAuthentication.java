package com.weaver.weaver_backend.configuration;

import com.weaver.weaver_backend.exception.ForbiddenException;
import com.weaver.weaver_backend.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.springframework.messaging.simp.stomp.StompCommand.CONNECT;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CLIENT-INBOUND-AUTHENTICATION")
public class ClientInboundAuthentication implements ChannelInterceptor {
    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (CONNECT.equals(accessor.getCommand())) {

                String authorization = accessor.getFirstNativeHeader("Authorization");

                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new MessageDeliveryException("Missing token");
                }

                String token = authorization.replace("Bearer ", "");

                try {
                    Claims claims = jwtUtils.extractClaims(token);
                    String jwtId = jwtUtils.getJwtId(claims);
                    if (jwtUtils.isTokenBlacklisted(jwtId)) {
                        throw new ForbiddenException("Cannot access to resource");
                    }
                    UUID userId = jwtUtils.getUserId(claims);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

                    log.info("Websocket connected - userId: {}", userId);
                } catch (JwtException ex) {
                    log.warn("Invalid JWT: {}", ex.getMessage());
                    throw new MessageDeliveryException("Unauthorized");
                }
            }
        }
        return message;
    }
}
