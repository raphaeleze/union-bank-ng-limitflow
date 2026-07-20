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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        account = new Account(customer, "0123456789", BigDecimal.valueOf(200_000), BigDecimal.valueOf(180_000));
        setId(account, UUID.randomUUID());

        lenient().when(limitRequestRepository.save(any(LimitRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitRequestRejectsAnAmountNotGreaterThanTheCurrentLimit() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.submitRequest(customer, account.getId(),
                BigDecimal.valueOf(200_000), "reason", true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void submitRequestRejectsWhenAnActiveRequestAlreadyExists() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submitRequest(customer, account.getId(),
                BigDecimal.valueOf(300_000), "reason", true))
                .isInstanceOf(ValidationException.class);

        verify(limitRequestRepository, never()).save(any());
    }

    @Test
    void submitRequestDeliversTheOtpThroughOtpDeliveryService() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(otpService.issue(any())).thenReturn("654321");

        service.submitRequest(customer, account.getId(), BigDecimal.valueOf(250_000), "reason", true);

        verify(otpDeliveryService).deliver(customer, "654321");
    }

    @Test
    void submitRequestStartsTheOtpStepAndSendsACode() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(otpService.issue(any(LimitRequest.class))).thenReturn("123456");

        LimitRequest result = service.submitRequest(customer, account.getId(),
                BigDecimal.valueOf(300_000), "vacation", true);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OTP_PENDING);
        verify(otpService).issue(any(LimitRequest.class));
        verify(notificationService).send(eq(customer), any(), any(), contains("123456"));
    }

    @Test
    void lowRiskRequestAutoApprovesAndRaisesTheAccountLimit() {
        LimitRequest limitRequest = new LimitRequest(account, BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(250_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Optional.of(limitRequest));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.LOW);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(account.getDailyLimit()).isEqualByComparingTo(BigDecimal.valueOf(250_000));
        verify(accountRepository).save(account);
    }

    @Test
    void mediumRiskRequestGoesToManualReviewInstead() {
        LimitRequest limitRequest = new LimitRequest(account, BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Optional.of(limitRequest));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.MEDIUM);

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.UNDER_REVIEW);
        verify(accountRepository, never()).save(any());
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
