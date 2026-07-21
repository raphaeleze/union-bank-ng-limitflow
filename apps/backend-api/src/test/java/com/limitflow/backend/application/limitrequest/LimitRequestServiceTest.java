package com.limitflow.backend.application.limitrequest;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.limitrequest.risk.RiskEngine;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpDeliveryService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.user.Role;
import com.limitflow.backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitRequestServiceTest {

    @Mock
    private LimitRequestRepository limitRequestRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private OtpDeliveryService otpDeliveryService;
    @Mock
    private RiskEngine riskEngine;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    private LimitRequestService service;
    private User customer;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new LimitRequestService(limitRequestRepository, accountRepository, otpService,
                otpDeliveryService, riskEngine, notificationService, auditService);

        customer = newUser(UUID.randomUUID(), Role.CUSTOMER);
        account = new Account(customer.getId(), "0123456789", BigDecimal.valueOf(200_000), BigDecimal.valueOf(180_000));
        setId(account, UUID.randomUUID());

        lenient().when(limitRequestRepository.save(any(LimitRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        lenient().when(auditService.record(any(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(auditService.record(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.empty());
        lenient().when(notificationService.send(any(UUID.class), any(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(otpDeliveryService.deliver(any(), anyString())).thenReturn(Mono.empty());
    }

    @Test
    void submitRequestRejectsAnAmountNotGreaterThanTheCurrentLimit() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(200_000), "reason", true))
                .verifyError(ValidationException.class);
    }

    @Test
    void submitRequestRejectsWhenAnActiveRequestAlreadyExists() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(300_000), "reason", true))
                .verifyError(ValidationException.class);

        verify(limitRequestRepository, never()).save(any());
    }

    @Test
    void submitRequestDeliversTheOtpThroughOtpDeliveryService() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(false));
        when(otpService.issue(any())).thenReturn(Mono.just("654321"));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(250_000), "reason", true))
                .expectNextCount(1)
                .verifyComplete();

        verify(otpDeliveryService).deliver(customer, "654321");
    }

    @Test
    void submitRequestStartsTheOtpStepAndSendsACode() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(false));
        when(otpService.issue(any(LimitRequest.class))).thenReturn(Mono.just("123456"));

        LimitRequest result = service.submitRequest(customer, account.getId(),
                BigDecimal.valueOf(300_000), "vacation", true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OTP_PENDING);
        verify(otpService).issue(any(LimitRequest.class));
        verify(notificationService).send(eq(customer.getId()), any(), any(), contains("123456"));
    }

    @Test
    void lowRiskRequestAutoApprovesAndRaisesTheAccountLimit() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(250_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.LOW);
        when(limitRequestRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(Mono.just(0L));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(account.getDailyLimit()).isEqualByComparingTo(BigDecimal.valueOf(250_000));
        verify(accountRepository).save(account);
    }

    @Test
    void mediumRiskRequestGoesToManualReviewInstead() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.MEDIUM);
        when(limitRequestRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(Mono.just(0L));

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.UNDER_REVIEW);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void cancelMarksAnActiveRequestCancelled() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(300_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.OTP_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));

        LimitRequest result = service.cancel(customer, limitRequest.getId()).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void cancelRejectsARequestThatIsAlreadyResolved() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(300_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.APPROVED);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));

        StepVerifier.create(service.cancel(customer, limitRequest.getId()))
                .verifyError(ValidationException.class);

        verify(limitRequestRepository, never()).save(any());
    }

    private User newUser(UUID id, Role role) {
        User user = mock(User.class, withSettings().lenient());
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
