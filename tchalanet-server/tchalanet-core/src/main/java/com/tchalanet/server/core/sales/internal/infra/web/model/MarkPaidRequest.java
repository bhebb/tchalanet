package com.tchalanet.server.core.sales.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record MarkPaidRequest(@NotBlank String reason, UUID performedBy) {}
