package com.tchalanet.server.features.ticketdelivery.model;

import jakarta.validation.constraints.NotNull;

public record DeliverTicketRequest(
    @NotNull TicketDeliveryChannel channel,
    @NotNull String recipient,
    String locale,
    Boolean includePdf,
    Boolean includeVerificationLink
) {}
