package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;
import com.tchalanet.server.common.types.id.SellerTerminalId;

public record PrintTicketRequest(
    SellerTerminalId sellerTerminalId,
    PrintOptionsRequest printOptionsRequest,
    boolean recordPrint,
    @Size(max = 500) String reprintReason,
    List<PrintDeliveryOption> deliveryOptions,
    String buyerPhoneNumber,
    @Email String buyerEmail,
    Locale buyerLocale
) {

    public PrintTicketRequest {
        // do not resolve defaults here; resolution must happen once in the service layer
        // leave printOptionsRequest nullable and let the service resolve it

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
