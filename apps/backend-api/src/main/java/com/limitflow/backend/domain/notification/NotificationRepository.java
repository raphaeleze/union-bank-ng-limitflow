package com.limitflow.backend.domain.notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
