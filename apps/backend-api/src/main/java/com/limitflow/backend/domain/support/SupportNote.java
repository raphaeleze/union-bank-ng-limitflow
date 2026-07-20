package com.limitflow.backend.domain.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("support_notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportNote {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("limit_request_id")
    private UUID limitRequestId;

    @Column("author_user_id")
    private UUID authorUserId;

    private String note;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public SupportNote(UUID limitRequestId, UUID authorUserId, String note) {
        this.limitRequestId = limitRequestId;
        this.authorUserId = authorUserId;
        this.note = note;
    }
}
