package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;

/** Reusable payload for admin {@code approve}/{@code reject} endpoints. */
public record AdminReasonRequest(@NotBlank String reason) {}
