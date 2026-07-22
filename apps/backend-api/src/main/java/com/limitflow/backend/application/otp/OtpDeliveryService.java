package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Sends the OTP via Twilio SMS when {@code limitflow.twilio.enabled=true} and the customer has
 * a phone number on file. Otherwise — and on any Twilio failure — falls back to a log line, the
 * same demo-mode behavior this codebase already had before real SMS delivery existed. Never
 * throws: a delivery failure must not block the limit-increase flow, since the code is already
 * persisted by {@link OtpService#issue}.
 */
@Slf4j
@Service
public class OtpDeliveryService {

    private final boolean enabled;
    private final String fromNumber;

    public OtpDeliveryService(
            @Value("${limitflow.twilio.enabled}") boolean enabled,
            @Value("${limitflow.twilio.account-sid}") String accountSid,
            @Value("${limitflow.twilio.auth-token}") String authToken,
            @Value("${limitflow.twilio.from-number}") String fromNumber) {
        this.enabled = enabled;
        this.fromNumber = fromNumber;
        if (enabled) {
            Twilio.init(accountSid, authToken);
        }
    }

    public Mono<Void> deliver(User customer, String code) {
        return Mono.fromRunnable(() -> deliverBlocking(customer, code))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void deliverBlocking(User customer, String code) {
        if (shouldSendViaTwilio(customer)) {
            try {
                Message.creator(new PhoneNumber(customer.getPhone()), new PhoneNumber(fromNumber),
                                "Your LimitFlow verification code is " + code)
                        .create();
                log.info("OTP sent via Twilio to {}", mask(customer.getPhone()));
                return;
            } catch (Exception e) {
                log.warn("Twilio send failed, falling back to log", e);
            }
        }
        log.info("OTP code (Twilio disabled or unavailable): {}", code);
    }

    boolean shouldSendViaTwilio(User customer) {
        return enabled && customer.getPhone() != null && !customer.getPhone().isBlank();
    }

    private String mask(String phone) {
        return phone.length() <= 4 ? phone : "***" + phone.substring(phone.length() - 4);
    }
}
