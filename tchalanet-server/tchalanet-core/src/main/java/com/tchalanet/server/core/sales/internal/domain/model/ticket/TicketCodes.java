package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.core.sales.api.model.value.PublicCode;
import com.tchalanet.server.core.sales.api.model.value.TicketCode;
import com.tchalanet.server.core.sales.api.model.value.VerificationCode;

public record TicketCodes(
    TicketCode ticketCode,
    PublicCode publicCode,
    VerificationCode verificationCode
) {
    public TicketCodes {
        if (ticketCode == null) {
            throw new IllegalArgumentException("ticketCode is required");
        }
        if (publicCode == null) {
            throw new IllegalArgumentException("publicCode is required");
        }
        if (verificationCode == null) {
            throw new IllegalArgumentException("verificationCode is required");
        }
    }

    public static TicketCodes of(TicketCode ticketCode, PublicCode publicCode, VerificationCode verificationCode) {
        return new TicketCodes(ticketCode, publicCode, verificationCode);
    }
    public static TicketCodes from(String ticketCode, String publicCode, String verificationCode) {
        return new TicketCodes(TicketCode.ofNullable(ticketCode), PublicCode.ofNullable(publicCode), VerificationCode.ofNullable(verificationCode));
    }
}
