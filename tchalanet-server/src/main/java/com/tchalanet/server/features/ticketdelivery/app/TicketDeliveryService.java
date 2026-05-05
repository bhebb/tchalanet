package com.tchalanet.server.features.ticketdelivery.app;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.features.ticketdelivery.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TicketDeliveryService {

  private static final Pattern EMAIL_RE =
      Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern PHONE_RE =
      Pattern.compile("^\\+?[1-9]\\d{6,14}$");

  private final TicketPrintReaderPort printReader;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final EdgeTicketDeliveryGateway gateway;

  public DeliverTicketResponse deliver(TicketId ticketId, DeliverTicketRequest request) {
    validateRequest(request);

    Locale locale = parseLocale(request.locale());
    var view = printReader.getTicketPrintView(ticketId, locale);

    String verificationUrl = urlBuilder.buildUrl(view.publicCode());
    boolean includeLink = request.includeVerificationLink() == null || request.includeVerificationLink();
    boolean includePdf = request.includePdf() != null && request.includePdf()
        && request.channel() == TicketDeliveryChannel.EMAIL;

    var lines = view.lines().stream()
        .map(l -> new EdgeDeliveryPayload.EdgeDeliveryLine(
            l.gameCode(), l.selection(), l.stake(), l.potentialPayout()))
        .toList();

    var payload = new EdgeDeliveryPayload(
        UUID.randomUUID(),
        request.channel().name(),
        request.recipient(),
        locale.toLanguageTag(),
        includePdf,
        includeLink,
        view.ticketCode(),
        view.publicCode(),
        verificationUrl,
        view.totalAmount(),
        null,
        view.createdAt(),
        view.outletName(),
        view.drawChannelLabel(),
        view.drawWhenLabel(),
        lines
    );

    var status = gateway.deliver(request, payload);
    return new DeliverTicketResponse(
        status,
        request.channel(),
        view.ticketCode(),
        view.publicCode(),
        status == TicketDeliveryStatus.ACCEPTED ? "Delivery accepted" : "Delivery failed"
    );
  }

  private void validateRequest(DeliverTicketRequest req) {
    if (req.channel() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channel is required");
    }
    if (req.recipient() == null || req.recipient().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipient is required");
    }
    switch (req.channel()) {
      case EMAIL -> {
        if (!EMAIL_RE.matcher(req.recipient().trim()).matches()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
        }
      }
      case SMS, WHATSAPP -> {
        if (!PHONE_RE.matcher(req.recipient().trim()).matches()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number (E.164 expected)");
        }
      }
    }
  }

  private Locale parseLocale(String tag) {
    if (tag == null || tag.isBlank()) return Locale.FRENCH;
    try { return Locale.forLanguageTag(tag); } catch (Exception e) { return Locale.FRENCH; }
  }
}
