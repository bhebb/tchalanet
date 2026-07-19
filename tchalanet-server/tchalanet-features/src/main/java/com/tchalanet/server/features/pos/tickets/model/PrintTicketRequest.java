package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record PrintTicketRequest(
    UUID sellerTerminalId,
    PrintOptionsRequest printOptionsRequest,
    boolean recordPrint,
    @Size(max = 500) String reprintReason,
    List<PrintDeliveryOption> deliveryOptions,
    String buyerPhoneNumber,
    @Email String buyerEmail,
    Locale buyerLocale) {

  public PrintTicketRequest {
    // do not resolve defaults here; resolution must happen once in the service layer
    // leave printOptionsRequest nullable and let the service resolve it

    deliveryOptions =
        deliveryOptions == null || deliveryOptions.isEmpty()
            ? List.of(PrintDeliveryOption.RETURN_FILE)
            : List.copyOf(deliveryOptions);
  }

  public boolean shouldReturnFile() {
    return deliveryOptions.contains(PrintDeliveryOption.RETURN_FILE);
  }

  public boolean shouldSendToBuyer() {
    return deliveryOptions.stream().anyMatch(PrintDeliveryOption::external);
  }
}
