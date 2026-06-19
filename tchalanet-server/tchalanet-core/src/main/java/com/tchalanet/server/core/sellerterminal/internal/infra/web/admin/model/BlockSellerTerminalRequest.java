package com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockSellerTerminalRequest(
    @NotBlank @Size(max = 500) String reason
) {}
