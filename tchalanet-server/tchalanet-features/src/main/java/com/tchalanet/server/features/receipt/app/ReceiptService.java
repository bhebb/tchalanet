package com.tchalanet.server.features.receipt.app;

import com.tchalanet.server.common.context.TchContextResolver;

import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.print.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.api.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.api.print.TicketVerificationUrlBuilder;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {

  private final TicketPrintReaderPort port;
  private final TchContextResolver contextResolver;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final DocumentApi documentApi;
  private final TicketReceiptFormatter pdfFormatter;
  private final TicketReceiptFormatter escPosFormatter;

  public ReceiptService(
      TicketPrintReaderPort port,
      TchContextResolver contextResolver,
      TicketVerificationUrlBuilder urlBuilder,
      DocumentApi documentApi,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter pdfFormatter,
      @Qualifier("ticketReceiptFormatterEscPos") TicketReceiptFormatter escPosFormatter) {
    this.port = port;
    this.contextResolver = contextResolver;
    this.urlBuilder = urlBuilder;
    this.documentApi = documentApi;
    this.pdfFormatter = pdfFormatter;
    this.escPosFormatter = escPosFormatter;
  }

  public byte[] renderPdf(TicketId ticketId) {
    return renderPdf(ticketId, currentLocale());
  }

  public byte[] renderPdf(TicketId ticketId, Locale locale) {
    var ticket = findTicket(ticketId, normalizeLocale(locale));
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    var text = receiptText(pdfFormatter.formatText(ticket, verifyUrl));
    byte[] qrBytes = documentApi.renderQrPng(verifyUrl, 300);
    return documentApi.renderReceiptPdf(text.title(), text.bodyLines(), qrBytes);
  }

  public byte[] renderEscPos(TicketId ticketId) {
    return renderEscPos(ticketId, currentLocale());
  }

  public byte[] renderEscPos(TicketId ticketId, Locale locale) {
    var ticket = findTicket(ticketId, normalizeLocale(locale));
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    var text = receiptText(escPosFormatter.formatText(ticket, verifyUrl));
    byte[] qrBytes = documentApi.renderQrEscPos(verifyUrl, 280);
    return documentApi.renderReceiptEscPos(text.title(), text.bodyLines(), qrBytes);
  }

  public byte[] renderQrPng(TicketId ticketId, int sizePx) {
    var ticket = findTicket(ticketId, currentLocale());
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    return documentApi.renderQrPng(verifyUrl, sizePx);
  }

  private Locale currentLocale() {
    var ctx = contextResolver.currentOrNull();
    return ctx == null || ctx.locale() == null ? Locale.FRENCH : ctx.locale();
  }

  private Locale normalizeLocale(Locale locale) {
    return locale == null ? Locale.FRENCH : locale;
  }

  private TicketPrintView findTicket(TicketId ticketId, Locale locale) {
    return port.findTicketPrintView(ticketId, locale)
        .orElseThrow(() -> ProblemRest.notFound("Ticket not found", ticketId));
  }

  private ReceiptText receiptText(String text) {
    var lines = text == null ? List.<String>of() : text.lines().toList();
    var title = lines.isEmpty() ? "Ticket Tchalanet" : lines.get(0);
    var body = lines.stream().skip(1).toList();
    return new ReceiptText(title, body);
  }

  private record ReceiptText(String title, List<String> bodyLines) {}
}
