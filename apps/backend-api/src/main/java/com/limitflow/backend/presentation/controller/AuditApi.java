package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@RequestMapping("/api/audit")
@Tag(name = "Audit")
public interface AuditApi {

    @GetMapping
    Flux<AuditLogResponse> auditLog();
}
