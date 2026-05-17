package com.tchalanet.server.core.sales.internal.infra.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to reject a pending ticket sale")
public record RejectTicketRequest(
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    @Schema(description = "Reason for rejection", example = "Suspected fraud")
    String reason
) {}
