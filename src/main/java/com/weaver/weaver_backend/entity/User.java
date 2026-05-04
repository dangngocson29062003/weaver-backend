package com.weaver.weaver_backend.entity;


import com.weaver.weaver_backend.common.AuthProvider;
import com.weaver.weaver_backend.common.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.userStatus == UserStatus.ACTIVE;
    }
}
