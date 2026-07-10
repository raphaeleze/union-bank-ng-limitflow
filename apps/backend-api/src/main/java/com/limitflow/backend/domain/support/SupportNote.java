package com.limitflow.backend.domain.support;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportNote {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "limit_request_id", nullable = false)
    private LimitRequest limitRequest;

    // Eager: always needed for display DTOs, and open-in-view is disabled.
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SupportNote(LimitRequest limitRequest, User author, String note) {
        this.limitRequest = limitRequest;
        this.author = author;
        this.note = note;
    }
}
