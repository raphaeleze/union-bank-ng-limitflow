package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID>, NotificationRepository {
}
