package com.limitflow.backend.application.support;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.support.SupportNoteRepository;
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
class SupportReviewServiceTest {

    @Mock
    private LimitRequestRepository limitRequestRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SupportNoteRepository supportNoteRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private OtpService otpService;

    private SupportReviewService service;
    private User supportAgent;
    private User manager;

    @BeforeEach
    void setUp() {
        service = new SupportReviewService(limitRequestRepository, accountRepository, supportNoteRepository,
                notificationService, auditService, otpService);

        supportAgent = newUser(Role.SUPPORT_AGENT);
        manager = newUser(Role.MANAGER);

        lenient().when(limitRequestRepository.save(any(LimitRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void supportAgentCannotApproveAHighRiskRequest() {
        LimitRequest limitRequest = underReviewRequest(RiskLevel.HIGH);
        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Optional.of(limitRequest));

        assertThatThrownBy(() -> service.approve(supportAgent, limitRequest.getId(), null))
                .isInstanceOf(ForbiddenException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void managerCannotApproveAMediumRiskRequest() {
        LimitRequest limitRequest = underReviewRequest(RiskLevel.MEDIUM);
        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Optional.of(limitRequest));

        assertThatThrownBy(() -> service.approve(manager, limitRequest.getId(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void supportAgentCanApproveAMediumRiskRequest() {
        LimitRequest limitRequest = underReviewRequest(RiskLevel.MEDIUM);
        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Optional.of(limitRequest));

        LimitRequest result = service.approve(supportAgent, limitRequest.getId(), null);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    private LimitRequest underReviewRequest(RiskLevel riskLevel) {
        Account account = new Account(mock(User.class), "0123456789",
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(180_000));
        setId(account, UUID.randomUUID());

        LimitRequest limitRequest = new LimitRequest(account, BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.setRiskLevel(riskLevel);
        limitRequest.transitionTo(RequestStatus.UNDER_REVIEW);
        return limitRequest;
    }

    private User newUser(Role role) {
        User user = mock(User.class, withSettings().lenient());
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
