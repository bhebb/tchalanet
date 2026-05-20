package com.tchalanet.server.core.sales.internal.infra.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to cancel a ticket sale")
public record CancelTicketRequest(
	@NotBlank(message = "Cancellation reason is required")
	@Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
	@Schema(description = "Reason for cancellation", example = "Customer requested cancellation")
	String reason
) {}
