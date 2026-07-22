package com.limitflow.backend.domain.push;

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
 * from existing rows just by checking for a null id — see {@link Persistable} on every other
 * entity in this codebase for why an explicit {@code isNew} flag is needed instead.
 */
@Table("push_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    @Column("expo_push_token")
    private String expoPushToken;

    private String platform;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Transient
    private boolean isNew = true;

    public PushToken(UUID userId, String expoPushToken, String platform) {
        this.userId = userId;
        this.expoPushToken = expoPushToken;
        this.platform = platform;
    }

    @PersistenceCreator
    PushToken(UUID id, UUID userId, String expoPushToken, String platform, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.expoPushToken = expoPushToken;
        this.platform = platform;
        this.createdAt = createdAt;
        this.isNew = false;
    }
}
