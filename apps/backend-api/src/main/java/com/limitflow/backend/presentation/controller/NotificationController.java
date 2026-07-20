package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    public Flux<NotificationResponse> notifications(User user) {
        return notificationService.findForUser(user.getId()).map(NotificationResponse::from);
    }
}
