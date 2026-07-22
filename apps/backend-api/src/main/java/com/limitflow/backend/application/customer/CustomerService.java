package com.limitflow.backend.application.customer;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final AccountRepository accountRepository;

    public Flux<Account> accountsFor(User customer) {
        return accountRepository.findByUserId(customer.getId());
    }

    public Mono<Account> primaryAccount(User customer) {
        return accountsFor(customer)
                .next()
                .switchIfEmpty(Mono.error(new NotFoundException("No account found for this customer")));
    }
}
