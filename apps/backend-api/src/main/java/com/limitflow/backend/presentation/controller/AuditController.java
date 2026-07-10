package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit")
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public List<AuditLogResponse> auditLog() {
        return auditService.findAll().stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
