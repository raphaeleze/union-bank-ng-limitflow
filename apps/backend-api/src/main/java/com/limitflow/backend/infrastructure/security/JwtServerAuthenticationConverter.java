package com.limitflow.backend.infrastructure.security;

import com.limitflow.backend.application.auth.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts the bearer token's user id and wraps it in an unauthenticated token; the actual user
 * lookup and role assignment happens in {@link JwtReactiveAuthenticationManager}. Paired with
 * {@link org.springframework.security.web.server.authentication.AuthenticationWebFilter} — the
 * standard Spring Security WebFlux building blocks for bearer-token auth — instead of a hand-rolled
 * {@code WebFilter} that manually calls {@code contextWrite(ReactiveSecurityContextHolder...)}.
 */
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final TokenService tokenService;

    public JwtServerAuthenticationConverter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(tokenService.extractUserId(header.substring(7)))
                .map(userId -> new UsernamePasswordAuthenticationToken(userId, null));
    }
}
