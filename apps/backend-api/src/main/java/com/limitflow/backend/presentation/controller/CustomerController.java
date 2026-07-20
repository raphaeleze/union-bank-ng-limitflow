package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.UserSummary;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CustomerController implements CustomerApi {

    @Override
    public Mono<UserSummary> me(User user) {
        return Mono.just(UserSummary.from(user));
    }
}
