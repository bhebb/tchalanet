package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDiagnosticPublicPolicyRequest(
    @Size(max = 64) String tenantCode, @NotBlank @Size(max = 64) String terminalCode) {}
