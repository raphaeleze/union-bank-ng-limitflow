package com.limitflow.backend.infrastructure.security;

import com.limitflow.backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtReactiveAuthenticationManagerTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void authenticateErrorsWithAuthenticationExceptionWhenUserNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        JwtReactiveAuthenticationManager manager = new JwtReactiveAuthenticationManager(userRepository);
        Authentication token = new UsernamePasswordAuthenticationToken(userId, null, List.of());

        StepVerifier.create(manager.authenticate(token))
                .verifyError(BadCredentialsException.class);
    }
}
