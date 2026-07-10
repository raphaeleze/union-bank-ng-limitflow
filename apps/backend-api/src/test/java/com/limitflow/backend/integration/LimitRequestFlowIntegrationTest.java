package com.limitflow.backend.integration;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"unchecked", "rawtypes"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
class LimitRequestFlowIntegrationTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("code is (\\d{6})");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void seededDashboardShowsTheActiveMediumRiskRequest() {
        String token = loginAs("customer@limitflow.demo");

        ResponseEntity<Map> response = restTemplate.exchange("/api/limits/current", HttpMethod.GET,
                authorized(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("dailyLimit")).isEqualTo(200000.0);
        assertThat(body.get("usedToday")).isEqualTo(180000.0);
        Map<String, Object> activeRequest = (Map<String, Object>) body.get("activeRequest");
        assertThat(activeRequest.get("status")).isEqualTo("UNDER_REVIEW");
        assertThat(activeRequest.get("riskLevel")).isEqualTo("MEDIUM");
    }

    @Test
    void lowRiskRequestWalksThroughOtpAndBiometricToAutomaticApproval() {
        String token = loginAs("customer@limitflow.demo");

        Map<String, Object> account = firstAccount(token);
        String accountId = (String) account.get("id");

        Map<String, Object> submitBody = Map.of(
                "accountId", accountId,
                "requestedLimit", 260000,
                "reason", "Paying a contractor",
                "knownDevice", true);
        ResponseEntity<Map> submitResponse = restTemplate.exchange("/api/limits/request", HttpMethod.POST,
                new HttpEntity<>(submitBody, authorizedHeaders(token)), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String requestId = (String) submitResponse.getBody().get("id");
        assertThat(submitResponse.getBody().get("status")).isEqualTo("OTP_PENDING");

        String otpCode = latestOtpCode(token);

        ResponseEntity<Map> otpResponse = restTemplate.exchange("/api/limits/" + requestId + "/otp/verify",
                HttpMethod.POST, new HttpEntity<>(Map.of("code", otpCode), authorizedHeaders(token)), Map.class);
        assertThat(otpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otpResponse.getBody().get("status")).isEqualTo("BIOMETRIC_PENDING");

        ResponseEntity<Map> biometricResponse = restTemplate.exchange("/api/limits/" + requestId + "/biometric/verify",
                HttpMethod.POST, new HttpEntity<>(Map.of("success", true), authorizedHeaders(token)), Map.class);
        assertThat(biometricResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(biometricResponse.getBody().get("status")).isEqualTo("APPROVED");
        assertThat(biometricResponse.getBody().get("riskLevel")).isEqualTo("LOW");

        List<Map<String, Object>> accounts = restTemplate.exchange(
                        "/api/accounts", HttpMethod.GET, authorized(token), List.class)
                .getBody();
        assertThat(accounts.get(0).get("dailyLimit")).isEqualTo(260000.0);
    }

    private Map<String, Object> firstAccount(String token) {
        ResponseEntity<List> response = restTemplate.exchange("/api/accounts", HttpMethod.GET,
                authorized(token), List.class);
        List<Map<String, Object>> accounts = response.getBody();
        return accounts.get(0);
    }

    private String latestOtpCode(String token) {
        ResponseEntity<List> response = restTemplate.exchange("/api/notifications", HttpMethod.GET,
                authorized(token), List.class);
        List<Map<String, Object>> notifications = response.getBody();
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
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "Password123!"), Map.class);
        return (String) response.getBody().get("token");
    }

    private HttpEntity<Void> authorized(String token) {
        return new HttpEntity<>(authorizedHeaders(token));
    }

    private HttpHeaders authorizedHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
