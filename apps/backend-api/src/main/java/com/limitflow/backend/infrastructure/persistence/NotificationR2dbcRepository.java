package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface NotificationR2dbcRepository extends R2dbcRepository<Notification, UUID>, NotificationRepository {
}
