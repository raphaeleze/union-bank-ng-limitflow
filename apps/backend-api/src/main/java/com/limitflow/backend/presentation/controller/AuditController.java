package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class AuditController implements AuditApi {

    private final AuditService auditService;

    @Override
    public Flux<AuditLogResponse> auditLog() {
        return auditService.findAll();
    }
}
