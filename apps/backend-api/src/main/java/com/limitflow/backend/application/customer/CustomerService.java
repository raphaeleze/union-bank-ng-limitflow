package com.limitflow.backend.application.customer;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final AccountRepository accountRepository;

    public List<Account> accountsFor(User customer) {
        return accountRepository.findByUserId(customer.getId());
    }

    public Account primaryAccount(User customer) {
        return accountsFor(customer).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No account found for this customer"));
    }
}
