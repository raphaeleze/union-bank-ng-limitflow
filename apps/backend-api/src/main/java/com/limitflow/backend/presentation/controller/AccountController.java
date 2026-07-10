package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.account.AccountResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts")
public class AccountController {

    private final CustomerService customerService;

    @GetMapping
    public List<AccountResponse> accounts(@AuthenticationPrincipal User user) {
        return customerService.accountsFor(user).stream()
                .map(AccountResponse::from)
                .toList();
    }
}
