package com.tchalanet.server.features.cashier.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.SoldTicketView;
import com.tchalanet.server.core.sales.api.print.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.api.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.features.cashier.model.CashierPrintFormat;
import com.tchalanet.server.features.cashier.model.CashierPrintableReceipt;
import com.tchalanet.server.features.cashier.model.CashierSellPrintRequest;
import com.tchalanet.server.features.cashier.model.CashierSellPrintResponse;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptRequest;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptResponse;
import com.tchalanet.server.features.cashier.model.CashierTicketView;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.receipt.app.TicketReceiptDocumentRequestFactory;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CashierService {

  private final CommandBus commandBus;
  private final TchContextResolver contextResolver;
  private final TicketPrintReaderPort printReader;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final DocumentApi documentApi;
  private final TicketReceiptFormatter pdfFormatter;
  private final TicketReceiptFormatter escPosFormatter;
  private final CommunicationApi outboundMessageGateway;
  private final SellerOperationalContextResolver sellerContextResolver;

  public CashierService(
      CommandBus commandBus,
      TchContextResolver contextResolver,
      TicketPrintReaderPort printReader,
      TicketVerificationUrlBuilder urlBuilder,
      DocumentApi documentApi,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter pdfFormatter,
      @Qualifier("ticketReceiptFormatterEscPos") TicketReceiptFormatter escPosFormatter,
      CommunicationApi outboundMessageGateway,
      SellerOperationalContextResolver sellerContextResolver) {
    this.commandBus = commandBus;
    this.contextResolver = contextResolver;
    this.printReader = printReader;
    this.urlBuilder = urlBuilder;
    this.documentApi = documentApi;
    this.pdfFormatter = pdfFormatter;
    this.escPosFormatter = escPosFormatter;
    this.outboundMessageGateway = outboundMessageGateway;
    this.sellerContextResolver = sellerContextResolver;
  }

  public CashierSellPrintResponse sellAndPrint(CashierSellPrintRequest request) {
    var ctx = contextResolver.currentOrThrow();
    var sellerContext = sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
        ctx,
        TerminalId.of(request.terminalId()),
        SellerOperation.SELL));
    var command = new SellTicketCommand(
        sellerContext.tenantId(),
        sellerContext.actorUserId(),
        sellerContext.terminalId(),
        sellerContext.outletId(),
        sellerContext.salesSessionId(),
        DrawId.of(request.drawId()),
        request.currency(),
        java.math.BigDecimal.ZERO,
        request.lines().stream()
            .map(line -> new SellTicketLineInput(
                line.gameCode(),
                line.selection(),
                line.betType(),
                line.betOption(),
                line.stake(),
                java.math.BigDecimal.ONE))
            .toList());

    var result = commandBus.execute(command);
    var ticket = toView(result.ticket());
    var receipt = result.outcome() == SellTicketOutcome.PENDING_APPROVAL
        ? null
        : renderReceipt(result.ticket().ticketId(), printFormat(request.printFormat()), ctx.locale());

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
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK ->
          new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), null, request.channelKey());
      case EMAIL, SMS, WHATSAPP -> new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), request.to(), null);
      case PUSH -> new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), request.to(), null);
    };

    outboundMessageGateway.enqueue(new SendOutboundMessageRequest(
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
        var text = receiptText(pdfFormatter.formatText(ticket, verifyUrl));
        var qrBytes = documentApi.renderQrPng(verifyUrl, 300);
        yield printable(
            format,
            "application/pdf",
            "ticket-" + ticketId + ".pdf",
            documentApi.renderReceiptPdf(text.title(), text.bodyLines(), qrBytes));
      }
      case ESC_POS -> {
        var text = receiptText(escPosFormatter.formatText(ticket, verifyUrl));
        var qrBytes = documentApi.renderQrEscPos(verifyUrl, 280);
        yield printable(
            format,
            "application/octet-stream",
            "ticket-" + ticketId + ".bin",
            documentApi.renderReceiptEscPos(text.title(), text.bodyLines(), qrBytes));
      }
      case QR_PNG -> printable(
          format,
          "image/png",
          "ticket-" + ticketId + ".png",
          documentApi.renderQrPng(verifyUrl, 280));
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

  private ReceiptText receiptText(String text) {
    var lines = text == null ? List.<String>of() : text.lines().toList();
    var title = lines.isEmpty() ? "Ticket Tchalanet" : lines.get(0);
    var body = lines.stream().skip(1).toList();
    return new ReceiptText(title, body);
  }

  private record ReceiptText(String title, List<String> bodyLines) {}

  private CashierTicketView toView(SoldTicketView ticket) {
    return new CashierTicketView(
        ticket.ticketId(),
        ticket.ticketCode(),
        ticket.publicCode(),
        ticket.saleStatus(),
        ticket.resultStatus(),
        ticket.settlementStatus(),
        ticket.totalAmount(),
        ticket.createdAt());
  }

  private CashierPrintFormat printFormat(CashierPrintFormat requested) {
    return requested == null ? CashierPrintFormat.PDF : requested;
  }

  private void validateRecipient(CashierSendReceiptRequest request) {
    if (request.channel() == null) {
      throw ProblemRest.badRequest("channel.required");
    }
    if (!isSlackChannel(request.channel()) && isBlank(request.to())) {
      throw ProblemRest.badRequest("recipient.to.required");
    }
  }

  private boolean isSlackChannel(CommunicationChannel channel) {
    return channel == CommunicationChannel.SLACK
        || channel == CommunicationChannel.SLACK_INTERNAL
        || channel == CommunicationChannel.SLACK_TENANT_WEBHOOK;
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
