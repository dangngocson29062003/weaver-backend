package com.weaver.weaver_backend.util;

import com.weaver.weaver_backend.common.TokenType;
import com.weaver.weaver_backend.dto.response.TokenResponse;
import com.weaver.weaver_backend.entity.User;
import com.weaver.weaver_backend.exception.GlobalExceptionHandler;
import com.weaver.weaver_backend.exception.UnauthorizedException;
import com.weaver.weaver_backend.service.IRedisTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret-key}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.two-fa-expiration}")
    private long twoFaExpiration;

    private final IRedisTokenService iRedisTokenService;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenResponse generateToken(User user, TokenType tokenType) {
        long expirationTime = switch (tokenType) {
            case ACCESS_TOKEN -> accessTokenExpiration;
            case REFRESH_TOKEN -> refreshTokenExpiration;
            case TWOFA_TOKEN -> twoFaExpiration;
        };
        String jwtID = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .setId(jwtID)
                .claim("email", user.getEmail())
                .claim("type", tokenType.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
        return new TokenResponse(token, jwtID, expirationTime / 1000);
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
            throw new UnauthorizedException("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw new UnauthorizedException("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            throw new UnauthorizedException("JWT claims string is empty");
        }
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public TokenType getType(Claims claims) {
        String typeStr = claims.get("type", String.class);
        return TokenType.valueOf(typeStr);
    }

    public Date getExpiration(Claims claims) {
        return claims.getExpiration();
    }

    public String getJwtId(Claims claims) {
        return claims.getId();
    }

    public boolean isTokenValid(Claims claims) {
        return claims != null && !isTokenExpired(claims) && !isTokenBlacklisted(claims.getId());
    }

    private boolean isTokenExpired(Claims claims) {
        return getExpiration(claims).before(new Date());
    }

    public boolean isTokenBlacklisted(String jwtId) {
        return iRedisTokenService.existsByJwtId(jwtId);
    }


}
