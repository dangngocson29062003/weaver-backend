package com.weaver.weaver_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String os;

    private String browser;

    private String deviceType;

    private String ipAddress;

    private String deviceId;

    private Instant lastActive;

    private Instant expiresAt;

    @Builder.Default
    private Boolean isTrusted = false;

    @Builder.Default
    private Boolean isRevoked = false;
}
