package com.tchalanet.server.features.cashier.print;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

public record PrintTicketRequest(
    PrintOutputFormat format,
    boolean recordPrint,
    @Size(max = 500) String reprintReason,
    List<PrintDeliveryOption> deliveryOptions,
    String buyerPhoneNumber,
    @Email String buyerEmail,
    Locale buyerLocale
) {

    public PrintTicketRequest {
        format = format == null ? PrintOutputFormat.PDF : format;

        deliveryOptions = deliveryOptions == null || deliveryOptions.isEmpty()
            ? List.of(PrintDeliveryOption.RETURN_FILE)
            : List.copyOf(deliveryOptions);

        if ((deliveryOptions.contains(PrintDeliveryOption.SMS)
            || deliveryOptions.contains(PrintDeliveryOption.WHATSAPP))
            && (buyerPhoneNumber == null || buyerPhoneNumber.isBlank())) {
            throw new IllegalArgumentException("buyerPhoneNumber is required for SMS/WHATSAPP delivery");
        }

        if (deliveryOptions.contains(PrintDeliveryOption.EMAIL)
            && (buyerEmail == null || buyerEmail.isBlank())) {
            throw new IllegalArgumentException("buyerEmail is required for EMAIL delivery");
        }
    }

    public boolean shouldReturnFile() {
        return deliveryOptions.contains(PrintDeliveryOption.RETURN_FILE);
    }

    public boolean shouldSendToBuyer() {
        return deliveryOptions.stream().anyMatch(PrintDeliveryOption::external);
    }
}
