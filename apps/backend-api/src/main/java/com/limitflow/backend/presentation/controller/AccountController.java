package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.account.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final CustomerService customerService;

    @Override
    public Flux<AccountResponse> accounts(User user) {
        return customerService.accountsFor(user).map(AccountResponse::from);
    }
}
