package com.weaver.weaver_backend.entity;


import com.weaver.weaver_backend.common.AuthProvider;
import com.weaver.weaver_backend.common.CredentialStatus;
import com.weaver.weaver_backend.common.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    @Column(name = "credential_status", nullable = false)
    @Builder.Default
    private CredentialStatus credentialStatus  = CredentialStatus.PASSWORD_SET;

    private String avatarUrl;

    @Column(name = "two_fa_enabled")
    @Builder.Default
    private Boolean twoFaEnabled = false;

    @Column(name = "two_fa_secret")
    private String twoFaSecret;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<NotificationUser> notifications = new ArrayList<>();
}
