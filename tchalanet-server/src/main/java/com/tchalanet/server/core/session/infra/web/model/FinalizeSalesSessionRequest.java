package com.tchalanet.server.core.session.infra.web.model;

import jakarta.validation.constraints.NotBlank;

public record FinalizeSalesSessionRequest(
    @NotBlank String reason
) {}
