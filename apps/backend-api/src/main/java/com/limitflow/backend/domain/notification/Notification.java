package com.limitflow.backend.domain.notification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    private NotificationType type;

    private String title;

    private String message;

    @Column("read_at")
    private Instant readAt;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public Notification(UUID userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public void markRead() {
        this.readAt = Instant.now();
    }
}
