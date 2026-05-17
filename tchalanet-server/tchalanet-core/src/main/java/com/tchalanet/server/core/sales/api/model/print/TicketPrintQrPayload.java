package com.tchalanet.server.core.sales.api.model.print;

public record TicketPrintQrPayload(
    String payloadVersion,
    String publicCode,
    String verificationCode,
    String verificationUrl,
    String payload
) {}
