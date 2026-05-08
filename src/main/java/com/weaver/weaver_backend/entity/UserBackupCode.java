package com.weaver.weaver_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_backup_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBackupCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String code;

    @Builder.Default
    private Boolean isUsed = false;

    private Instant usedAt;

    @CreationTimestamp
    private Instant createdAt = Instant.now();
}
