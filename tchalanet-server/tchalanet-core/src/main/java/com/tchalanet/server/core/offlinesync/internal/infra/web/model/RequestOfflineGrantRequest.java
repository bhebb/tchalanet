package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestOfflineGrantRequest(
    @NotNull UUID deviceId,
    @NotBlank String devicePublicKey,
    @NotBlank String keyId
) {}
