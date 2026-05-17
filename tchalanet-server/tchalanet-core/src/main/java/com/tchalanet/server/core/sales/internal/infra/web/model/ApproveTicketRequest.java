package com.tchalanet.server.core.sales.internal.infra.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to approve a pending ticket sale")
public record ApproveTicketRequest(
    @Size(max = 500, message = "Reason must be at most 500 characters")
    @Schema(description = "Optional reason for approval", example = "Buyer verified")
    String reason
) {}

