package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.UserSummary;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer")
public class CustomerController {

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal User user) {
        return UserSummary.from(user);
    }
}
