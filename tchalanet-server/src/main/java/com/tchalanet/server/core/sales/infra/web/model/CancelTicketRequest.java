package com.tchalanet.server.core.sales.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CancelTicketRequest(@NotBlank String reason, UUID performedBy) {}
