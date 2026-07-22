package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpDeliveryServiceTest {

    @Test
    void shouldSendViaTwilioIsFalseWhenDisabled() {
        OtpDeliveryService service = new OtpDeliveryService(false, "", "", "");
        User customer = newCustomer("+2348012345678");

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsFalseWhenPhoneIsMissing() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer(null);

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsFalseWhenPhoneIsBlank() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer("   ");

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsTrueWhenEnabledAndPhonePresent() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer("+2348012345678");

        assertThat(service.shouldSendViaTwilio(customer)).isTrue();
    }

    @Test
    void deliverFallsBackToLoggingWithoutThrowingWhenDisabled() {
        OtpDeliveryService service = new OtpDeliveryService(false, "", "", "");
        User customer = newCustomer(null);

        StepVerifier.create(service.deliver(customer, "123456")).verifyComplete();
    }

    private User newCustomer(String phone) {
        User user = mock(User.class);
        lenient().when(user.getPhone()).thenReturn(phone);
        return user;
    }
}
