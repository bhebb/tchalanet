package com.tchalanet.server.features.cashier.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.service.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
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
  private final TicketReceiptDocumentRequestFactory documentRequestFactory;
  private final CommunicationApi outboundMessageGateway;
  private final SellerOperationalContextResolver sellerContextResolver;

  public CashierService(
      CommandBus commandBus,
      TchContextResolver contextResolver,
      TicketPrintReaderPort printReader,
      TicketVerificationUrlBuilder urlBuilder,
      DocumentApi documentApi,
      TicketReceiptDocumentRequestFactory documentRequestFactory,
      CommunicationApi outboundMessageGateway,
      SellerOperationalContextResolver sellerContextResolver) {
    this.commandBus = commandBus;
    this.contextResolver = contextResolver;
    this.printReader = printReader;
    this.urlBuilder = urlBuilder;
    this.documentApi = documentApi;
    this.documentRequestFactory = documentRequestFactory;
    this.outboundMessageGateway = outboundMessageGateway;
    this.sellerContextResolver = sellerContextResolver;
  }

  public CashierSellPrintResponse sellAndPrint(CashierSellPrintRequest request) {
    var ctx = contextResolver.currentOrThrow();
    sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
        ctx,
        TerminalId.of(request.terminalId()),
        SellerOperation.SELL));
    var lines = request.lines();
    var command = new SellTicketCommand(
        DrawId.of(request.drawId()),
        null,
        CurrencyCode.of(request.currency()),
        java.util.stream.IntStream.range(0, lines.size())
            .mapToObj(i -> {
                var line = lines.get(i);
                return new SellTicketLineInput(
                    i + 1,
                    line.gameCode(),
                    line.betType(),
                    line.selection(),
                    line.betOption(),
                    line.stake());
            })
            .toList(),
        SaleCommunicationOptions.none());

    var result = commandBus.execute(command);
    var ticket = toView(result.ticket());
    CashierPrintableReceipt receipt = result.outcome() == SellTicketOutcome.PENDING_APPROVAL
        ? null
        : renderReceipt(result.ticket().identity().id(), printFormat(request.printFormat()), ctx.locale());

    return new CashierSellPrintResponse(ticket, result.outcome(), result.approvalRequestId(), receipt);
  }

  public CashierSendReceiptResponse sendReceipt(TicketId ticketId, CashierSendReceiptRequest request) {
    var ctx = contextResolver.currentOrThrow();
    var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();
    var ticket = findTicket(ticketId);
    validateRecipient(request);
    var verifyUrl = urlBuilder.buildUrl(ticket.identity().publicCode());
    var includeLink = request.includeVerificationLink() == null || request.includeVerificationLink();

    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("eventId", "ticket-receipt-" + ticket.identity().ticketId());
    metadata.put("requestId", ctx.requestId());
    metadata.put("idempotencyKey", ctx.idempotencyKey());
    metadata.put("severity", "INFO");
    metadata.put("title", "Ticket Tchalanet " + ticket.identity().ticketCode());
    metadata.put("message", receiptMessage(ticket, includeLink ? verifyUrl : null));
    metadata.put("ticketCode", ticket.identity().ticketCode());
    metadata.put("publicCode", ticket.identity().publicCode());
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
    var normalized = locale == null ? Locale.FRENCH : locale;
    var ticket = findTicket(ticketId);
    var verifyUrl = urlBuilder.buildUrl(ticket.identity().publicCode());
    return switch (format) {
      case PDF -> {
        var request = documentRequestFactory.receiptRequest(ticket, verifyUrl, DocumentFormat.PDF, normalized);
        yield printable(
            format,
            "application/pdf",
            "ticket-" + ticketId + ".pdf",
            documentApi.render(request).bytes());
      }
      case ESC_POS -> {
        var request = documentRequestFactory.receiptRequest(ticket, verifyUrl, DocumentFormat.ESC_POS, normalized);
        yield printable(
            format,
            "application/octet-stream",
            "ticket-" + ticketId + ".bin",
            documentApi.render(request).bytes());
      }
      case QR_PNG -> printable(
          format,
          "image/png",
          "ticket-" + ticketId + ".png",
          documentApi.render(documentRequestFactory.qrPngRequest(verifyUrl, 280, normalized)).bytes());
    };
  }

  private CashierPrintableReceipt printable(
      CashierPrintFormat format,
      String contentType,
      String filename,
      byte[] bytes) {
    return new CashierPrintableReceipt(format, contentType, filename, Base64.getEncoder().encodeToString(bytes));
  }

  private TicketPrintView findTicket(TicketId ticketId) {
    return printReader.findPrintViewRequired(ticketId);
  }

  private CashierTicketView toView(Ticket ticket) {
    return new CashierTicketView(
        ticket.identity().id(),
        ticket.codes().ticketCode().value(),
        ticket.codes().publicCode().value(),
        ticket.lifecycle().sale().status(),
        ticket.lifecycle().result().status(),
        ticket.lifecycle().settlement().status(),
        ticket.money().totalAmount().amount(),
        ticket.audit().createdAt());
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
    var message = "Ticket: " + ticket.identity().ticketCode() + "\n"
        + "Montant: " + ticket.money().totalAmount();
    return verifyUrl == null ? message : message + "\nVerifier: " + verifyUrl;
  }
}
