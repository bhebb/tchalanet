package com.tchalanet.server.features.ticketdelivery.model;

public record DeliverTicketResponse(
    TicketDeliveryStatus status,
    TicketDeliveryChannel channel,
    String ticketCode,
    String publicCode,
    String message
) {}
