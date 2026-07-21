package com.limitflow.backend.integration;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
@Import(EmbeddedR2dbcConfig.class)
class LimitRequestFlowIntegrationTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("code is (\\d{6})");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void seededDashboardShowsTheActiveMediumRiskRequest() {
        String token = loginAs("customer@limitflow.demo");

        Map<String, Object> body = webTestClient.get().uri("/api/limits/current")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(body.get("dailyLimit")).isEqualTo(200000.0);
        assertThat(body.get("usedToday")).isEqualTo(180000.0);
        Map<String, Object> activeRequest = (Map<String, Object>) body.get("activeRequest");
        assertThat(activeRequest.get("status")).isEqualTo("UNDER_REVIEW");
        assertThat(activeRequest.get("riskLevel")).isEqualTo("MEDIUM");
    }

    @Test
    void lowRiskRequestWalksThroughOtpAndBiometricToAutomaticApproval() {
        resolveSeededRequest();

        String token = loginAs("customer@limitflow.demo");

        Map<String, Object> account = firstAccount(token);
        String accountId = (String) account.get("id");

        Map<String, Object> submitBody = Map.of(
                "accountId", accountId,
                "requestedLimit", 600000,
                "reason", "Paying a contractor",
                "knownDevice", true);
        Map<String, Object> submitResponse = webTestClient.post().uri("/api/limits/request")
                .header("Authorization", "Bearer " + token)
                .bodyValue(submitBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        String requestId = (String) submitResponse.get("id");
        assertThat(submitResponse.get("status")).isEqualTo("OTP_PENDING");

        String otpCode = latestOtpCode(token);

        Map<String, Object> otpResponse = webTestClient.post().uri("/api/limits/" + requestId + "/otp/verify")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("code", otpCode))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(otpResponse.get("status")).isEqualTo("BIOMETRIC_PENDING");

        Map<String, Object> biometricResponse = webTestClient.post().uri("/api/limits/" + requestId + "/biometric/verify")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("success", true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(biometricResponse.get("status")).isEqualTo("APPROVED");
        assertThat(biometricResponse.get("riskLevel")).isEqualTo("LOW");

        List<Map<String, Object>> accounts = (List<Map<String, Object>>) (List<?>) webTestClient.get().uri("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(accounts.get(0).get("dailyLimit")).isEqualTo(600000.0);
    }

    private void resolveSeededRequest() {
        String supportToken = loginAs("support@limitflow.demo");
        webTestClient.post().uri("/api/support/requests/55555555-5555-5555-5555-555555555555/approve")
                .header("Authorization", "Bearer " + supportToken)
                .bodyValue(Map.of())
                .exchange();
    }

    private Map<String, Object> firstAccount(String token) {
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) (List<?>) webTestClient.get().uri("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        return accounts.get(0);
    }

    private String latestOtpCode(String token) {
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) (List<?>) webTestClient.get().uri("/api/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        String message = notifications.stream()
                .filter(n -> "OTP_SENT".equals(n.get("type")))
                .findFirst()
                .map(n -> (String) n.get("message"))
                .orElseThrow();
        Matcher matcher = CODE_PATTERN.matcher(message);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String loginAs(String email) {
        Map<String, Object> body = webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", email, "password", "Password123!"))
                .exchange()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return (String) body.get("token");
    }
}
