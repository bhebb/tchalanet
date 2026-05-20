package com.tchalanet.server.core.sales.api.model.communication;

import java.util.Locale;

public record SaleCommunicationOptions(
    boolean sendSms,
    boolean sendWhatsapp,
    boolean sendEmail,
    String buyerPhoneNumber,
    String buyerEmail,
    Locale buyerLocale
) {
    public SaleCommunicationOptions {
        boolean phoneRequired = sendSms || sendWhatsapp;

        if (phoneRequired && (buyerPhoneNumber == null || buyerPhoneNumber.isBlank())) {
            throw new IllegalArgumentException(
                "buyerPhoneNumber is required when SMS or WhatsApp is requested"
            );
        }

        if (sendEmail && (buyerEmail == null || buyerEmail.isBlank())) {
            throw new IllegalArgumentException(
                "buyerEmail is required when email is requested"
            );
        }
    }

    public static SaleCommunicationOptions none() {
        return new SaleCommunicationOptions(false, false, false, null, null, null);
    }

    public boolean isEmpty() {
        return !sendSms && !sendWhatsapp && !sendEmail;
    }
}
