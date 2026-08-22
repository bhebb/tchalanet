package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClientDiagnosticPublicBatchRequest(
    @Size(max = 64) String tenantCode,
    @NotBlank @Size(max = 64) String terminalCode,
    @NotEmpty @Size(max = 20) List<@Valid ClientDiagnosticEventRequest> events) {}
