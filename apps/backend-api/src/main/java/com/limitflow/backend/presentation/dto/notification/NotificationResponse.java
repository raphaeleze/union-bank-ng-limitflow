package com.limitflow.backend.presentation.dto.notification;

import com.limitflow.backend.domain.notification.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}
