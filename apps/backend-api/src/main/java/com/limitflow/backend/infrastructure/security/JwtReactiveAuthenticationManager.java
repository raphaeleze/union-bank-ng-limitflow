package com.limitflow.backend.infrastructure.security;

import com.limitflow.backend.domain.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Looks up the user id extracted by {@link JwtServerAuthenticationConverter} and produces a fully
 * authenticated token with the user's role as a granted authority.
 */
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final UserRepository userRepository;

    public JwtReactiveAuthenticationManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return userRepository.findById(userId)
                .<Authentication>map(user -> new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))))
                .switchIfEmpty(Mono.error(new BadCredentialsException("User for token no longer exists: " + userId)));
    }
}
