package com.tchalanet.server.features.ticketverify.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TicketVerifyRequest(
    @NotBlank
    @Size(min = 6, max = 32)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Invalid public code printOptionsRequest")
    String publicCode
) {}
