package com.weaver.weaver_backend.entity;

import com.weaver.weaver_backend.common.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "notification", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<NotificationUser> recipients = new ArrayList<>();

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "action_url")
    private String actionUrl;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @CreatedDate
    private LocalDateTime createdAt;

    public void addRecipients(User user) {
        recipients.add(NotificationUser.builder()
                .notification(this)
                .user(user)
                .isRead(false)
                .build());
    }
}
