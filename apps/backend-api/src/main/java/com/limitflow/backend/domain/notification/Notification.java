package com.limitflow.backend.domain.notification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code id} is client-assigned (not DB-generated), so Spring Data R2DBC can't tell new
 * from existing rows just by checking for a null id — it would otherwise emit an UPDATE for a
 * brand-new, never-persisted entity, silently affecting zero rows instead of inserting.
 * Implementing {@link Persistable} with an explicit {@code isNew} flag fixes that: the business
 * constructor leaves it {@code true}, while the {@link PersistenceCreator} constructor Spring
 * Data uses to rehydrate rows read back from the database sets it {@code false}.
 */
@Table("notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification implements Persistable<UUID> {

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

    @Transient
    private boolean isNew = true;

    public Notification(UUID userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    @PersistenceCreator
    Notification(UUID id, UUID userId, NotificationType type, String title, String message, Instant readAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public void markRead() {
        this.readAt = Instant.now();
    }
}
