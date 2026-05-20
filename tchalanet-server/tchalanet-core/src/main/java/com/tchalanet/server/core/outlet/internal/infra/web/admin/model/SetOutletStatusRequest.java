package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotBlank;

public record SetOutletStatusRequest(
    @NotBlank String status,
    String reason
) {}
