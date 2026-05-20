package com.tchalanet.server.features.receipt.app;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.internal.application.service.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {

  private final TicketPrintReaderPort port;
  private final TchContextResolver contextResolver;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final DocumentApi documentApi;
  private final TicketReceiptDocumentRequestFactory requestFactory;

  public ReceiptService(
      TicketPrintReaderPort port,
      TchContextResolver contextResolver,
      TicketVerificationUrlBuilder urlBuilder,
      DocumentApi documentApi,
      TicketReceiptDocumentRequestFactory requestFactory) {
    this.port = port;
    this.contextResolver = contextResolver;
    this.urlBuilder = urlBuilder;
    this.documentApi = documentApi;
    this.requestFactory = requestFactory;
  }

  public byte[] renderPdf(TicketId ticketId) {
    return renderPdf(ticketId, currentLocale());
  }

  public byte[] renderPdf(TicketId ticketId, Locale locale) {
    var normalized = normalizeLocale(locale);
    var ticket = findTicket(ticketId);
    var verifyUrl = urlBuilder.buildUrl(ticket.identity().publicCode());
    var request = requestFactory.receiptRequest(ticket, verifyUrl, DocumentFormat.PDF, normalized);
    return documentApi.render(request).bytes();
  }

  public byte[] renderEscPos(TicketId ticketId) {
    return renderEscPos(ticketId, currentLocale());
  }

  public byte[] renderEscPos(TicketId ticketId, Locale locale) {
    var normalized = normalizeLocale(locale);
    var ticket = findTicket(ticketId);
    var verifyUrl = urlBuilder.buildUrl(ticket.identity().publicCode());
    var request =
        requestFactory.receiptRequest(ticket, verifyUrl, DocumentFormat.ESC_POS, normalized);
    return documentApi.render(request).bytes();
  }

  public byte[] renderQrPng(TicketId ticketId, int sizePx) {
    var locale = currentLocale();
    var ticket = findTicket(ticketId);
    var verifyUrl = urlBuilder.buildUrl(ticket.identity().publicCode());
    return documentApi.render(requestFactory.qrPngRequest(verifyUrl, sizePx, locale)).bytes();
  }

  private Locale currentLocale() {
    var ctx = contextResolver.currentOrNull();
    return ctx == null || ctx.locale() == null ? Locale.FRENCH : ctx.locale();
  }

  private Locale normalizeLocale(Locale locale) {
    return locale == null ? Locale.FRENCH : locale;
  }

  private TicketPrintView findTicket(TicketId ticketId) {
    return port.findPrintViewRequired(ticketId);
  }
}
