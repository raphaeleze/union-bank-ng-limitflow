package com.limitflow.backend.integration;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
@Import(EmbeddedR2dbcConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void seededCustomerCanLogIn() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", "customer@limitflow.demo", "password", "Password123!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.token").exists();
    }

    @Test
    void wrongPasswordIsRejected() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", "customer@limitflow.demo", "password", "wrong-password"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
