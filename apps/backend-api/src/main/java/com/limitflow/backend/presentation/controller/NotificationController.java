package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.notification.NotificationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> notifications(@AuthenticationPrincipal User user) {
        return notificationService.findForUser(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
