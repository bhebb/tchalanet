package com.tchalanet.server.features.cashier.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.communication.api.CommunicationChannel;
import com.tchalanet.server.common.communication.api.OutboundMessageGateway;
import com.tchalanet.server.common.communication.api.OutboundMessageRequest;
import com.tchalanet.server.common.communication.api.OutboundRecipient;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.document.escpos.EscPosBuilder;
import com.tchalanet.server.common.document.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.common.document.qr.QrRenderer;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.application.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.application.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.features.cashier.model.CashierPrintFormat;
import com.tchalanet.server.features.cashier.model.CashierPrintableReceipt;
import com.tchalanet.server.features.cashier.model.CashierSellPrintRequest;
import com.tchalanet.server.features.cashier.model.CashierSellPrintResponse;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptRequest;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptResponse;
import com.tchalanet.server.features.cashier.model.CashierTicketView;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CashierService {

  private final CommandBus commandBus;
  private final TchContextResolver contextResolver;
  private final TicketPrintReaderPort printReader;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final QrRenderer qrPng;
  private final QrRenderer qrEscPos;
  private final ReceiptPdfRenderer pdf;
  private final EscPosBuilder escpos;
  private final TicketReceiptFormatter pdfFormatter;
  private final TicketReceiptFormatter escPosFormatter;
  private final OutboundMessageGateway outboundMessageGateway;

  public CashierService(
      CommandBus commandBus,
      TchContextResolver contextResolver,
      TicketPrintReaderPort printReader,
      TicketVerificationUrlBuilder urlBuilder,
      @Qualifier("qrPngRenderer") QrRenderer qrPng,
      @Qualifier("qrEscPosRenderer") QrRenderer qrEscPos,
      ReceiptPdfRenderer pdf,
      EscPosBuilder escpos,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter pdfFormatter,
      @Qualifier("ticketReceiptFormatterEscPos") TicketReceiptFormatter escPosFormatter,
      OutboundMessageGateway outboundMessageGateway) {
    this.commandBus = commandBus;
    this.contextResolver = contextResolver;
    this.printReader = printReader;
    this.urlBuilder = urlBuilder;
    this.qrPng = qrPng;
    this.qrEscPos = qrEscPos;
    this.pdf = pdf;
    this.escpos = escpos;
    this.pdfFormatter = pdfFormatter;
    this.escPosFormatter = escPosFormatter;
    this.outboundMessageGateway = outboundMessageGateway;
  }

  public CashierSellPrintResponse sellAndPrint(CashierSellPrintRequest request) {
    var ctx = contextResolver.currentOrThrow();
    var command = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        TerminalId.of(request.terminalId()),
        ctx.currentUserIdRequired(),
        DrawId.of(request.drawId()),
        request.lines().stream()
            .map(line -> new SellTicketCommand.LineCommand(
                line.gameCode(),
                line.selection(),
                line.stake(),
                line.betType(),
                line.betOption()))
            .toList(),
        request.currency());

    var result = commandBus.execute(command);
    var ticket = toView(result.ticket());
    var receipt = result.outcome() == SellTicketOutcome.PENDING_APPROVAL
        ? null
        : renderReceipt(result.ticket().getId(), printFormat(request.printFormat()), ctx.locale());

    return new CashierSellPrintResponse(ticket, result.outcome(), result.approvalRequestId(), receipt);
  }

  public CashierSendReceiptResponse sendReceipt(TicketId ticketId, CashierSendReceiptRequest request) {
    var ctx = contextResolver.currentOrThrow();
    var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();
    var ticket = findTicket(ticketId, locale);
    validateRecipient(request);
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    var includeLink = request.includeVerificationLink() == null || request.includeVerificationLink();

    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("eventId", "ticket-receipt-" + ticket.ticketId());
    metadata.put("requestId", ctx.requestId());
    metadata.put("idempotencyKey", ctx.idempotencyKey());
    metadata.put("severity", "INFO");
    metadata.put("title", "Ticket Tchalanet " + ticket.ticketCode());
    metadata.put("message", receiptMessage(ticket, includeLink ? verifyUrl : null));
    metadata.put("ticketCode", ticket.ticketCode());
    metadata.put("publicCode", ticket.publicCode());
    if (includeLink) {
      metadata.put("verificationUrl", verifyUrl);
    }

    var recipient = switch (request.channel()) {
      case SLACK -> new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), null, request.channelKey());
      case EMAIL, SMS, WHATSAPP -> new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), request.to(), null);
    };

    outboundMessageGateway.send(new OutboundMessageRequest(
        "TICKET_RECEIPT",
        request.channel(),
        recipient,
        locale,
        metadata));

    return new CashierSendReceiptResponse(ticketId, request.channel(), true);
  }

  private CashierPrintableReceipt renderReceipt(TicketId ticketId, CashierPrintFormat format, Locale locale) {
    var ticket = findTicket(ticketId, locale == null ? Locale.FRENCH : locale);
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    return switch (format) {
      case PDF -> {
        var model = pdfFormatter.formatModel(ticket, verifyUrl);
        var qrBytes = qrPng.render(verifyUrl, new QrRenderer.QrRenderSpec(300));
        yield printable(format, "application/pdf", "ticket-" + ticketId + ".pdf", pdf.render(model, qrBytes));
      }
      case ESC_POS -> {
        var model = escPosFormatter.formatModel(ticket, verifyUrl);
        var parts = new ArrayList<byte[]>();
        parts.add(escpos.init());
        parts.add(escpos.alignLeft());
        for (var line : model.lines()) {
          for (var span : line.spans()) {
            parts.add(span.bold() ? escpos.boldOn() : escpos.boldOff());
            parts.add(escpos.text(span.text()));
          }
          parts.add(escpos.boldOff());
          parts.add(escpos.lf());
        }
        parts.add(escpos.alignCenter());
        parts.add(qrEscPos.render(verifyUrl, new QrRenderer.QrRenderSpec(280)));
        parts.add(escpos.alignLeft());
        parts.add(escpos.cut());
        yield printable(
            format,
            "application/octet-stream",
            "ticket-" + ticketId + ".bin",
            escpos.concat(parts.toArray(new byte[0][])));
      }
      case QR_PNG -> printable(
          format,
          "image/png",
          "ticket-" + ticketId + ".png",
          qrPng.render(verifyUrl, new QrRenderer.QrRenderSpec(280)));
    };
  }

  private CashierPrintableReceipt printable(
      CashierPrintFormat format,
      String contentType,
      String filename,
      byte[] bytes) {
    return new CashierPrintableReceipt(format, contentType, filename, Base64.getEncoder().encodeToString(bytes));
  }

  private TicketPrintView findTicket(TicketId ticketId, Locale locale) {
    return printReader.findTicketPrintView(ticketId, locale)
        .orElseThrow(() -> ProblemRest.notFound("Ticket not found", ticketId));
  }

  private CashierTicketView toView(Ticket ticket) {
    return new CashierTicketView(
        ticket.getId(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getSaleStatus(),
        ticket.getResultStatus(),
        ticket.getSettlementStatus(),
        ticket.getTotalAmount(),
        ticket.getCreatedAt());
  }

  private CashierPrintFormat printFormat(CashierPrintFormat requested) {
    return requested == null ? CashierPrintFormat.PDF : requested;
  }

  private void validateRecipient(CashierSendReceiptRequest request) {
    if (request.channel() == null) {
      throw ProblemRest.badRequest("channel.required");
    }
    if (request.channel() != CommunicationChannel.SLACK && isBlank(request.to())) {
      throw ProblemRest.badRequest("recipient.to.required");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String receiptMessage(TicketPrintView ticket, String verifyUrl) {
    var message = "Ticket: " + ticket.ticketCode() + "\n"
        + "Montant: " + ticket.totalAmount();
    return verifyUrl == null ? message : message + "\nVerifier: " + verifyUrl;
  }
}
