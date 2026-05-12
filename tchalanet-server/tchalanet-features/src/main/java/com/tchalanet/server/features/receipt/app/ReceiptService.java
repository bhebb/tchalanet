package com.tchalanet.server.features.receipt.app;

import com.tchalanet.server.common.document.escpos.EscPosBuilder;
import com.tchalanet.server.common.document.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.common.document.qr.QrRenderer;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.application.print.TicketVerificationUrlBuilder;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {

  private final TicketPrintReaderPort port;
  private final TchContextResolver contextResolver;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final QrRenderer qrPng;
  private final QrRenderer qrEscPos;
  private final ReceiptPdfRenderer pdf;
  private final EscPosBuilder escpos;
  private final TicketReceiptFormatter pdfFormatter;
  private final TicketReceiptFormatter escPosFormatter;

  public ReceiptService(
      TicketPrintReaderPort port,
      TchContextResolver contextResolver,
      TicketVerificationUrlBuilder urlBuilder,
      @Qualifier("qrPngRenderer") QrRenderer qrPng,
      @Qualifier("qrEscPosRenderer") QrRenderer qrEscPos,
      ReceiptPdfRenderer pdf,
      EscPosBuilder escpos,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter pdfFormatter,
      @Qualifier("ticketReceiptFormatterEscPos") TicketReceiptFormatter escPosFormatter) {
    this.port = port;
    this.contextResolver = contextResolver;
    this.urlBuilder = urlBuilder;
    this.qrPng = qrPng;
    this.qrEscPos = qrEscPos;
    this.pdf = pdf;
    this.escpos = escpos;
    this.pdfFormatter = pdfFormatter;
    this.escPosFormatter = escPosFormatter;
  }

  public byte[] renderPdf(TicketId ticketId) {
    return renderPdf(ticketId, currentLocale());
  }

  public byte[] renderPdf(TicketId ticketId, Locale locale) {
    var ticket = findTicket(ticketId, normalizeLocale(locale));
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    var model = pdfFormatter.formatModel(ticket, verifyUrl);
    byte[] qrBytes = qrPng.render(verifyUrl, new QrRenderer.QrRenderSpec(300));
    return pdf.render(model, qrBytes);
  }

  public byte[] renderEscPos(TicketId ticketId) {
    return renderEscPos(ticketId, currentLocale());
  }

  public byte[] renderEscPos(TicketId ticketId, Locale locale) {
    var ticket = findTicket(ticketId, normalizeLocale(locale));
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    var model = escPosFormatter.formatModel(ticket, verifyUrl);

    var parts = new ArrayList<byte[]>();
    parts.add(escpos.init());
    parts.add(escpos.alignLeft());

    parts.add(escpos.alignCenter());
    parts.add(escpos.boldOn());
    parts.add(escpos.text(model.title()));
    parts.add(escpos.boldOff());
    parts.add(escpos.lf());
    parts.add(escpos.alignLeft());

    for (var line : model.lines()) {
      for (var span : line.spans()) {
        parts.add(span.bold() ? escpos.boldOn() : escpos.boldOff());
        parts.add(escpos.text(span.text()));
      }
      parts.add(escpos.boldOff());
      parts.add(escpos.lf());
    }

    byte[] qrBytes = qrEscPos.render(verifyUrl, new QrRenderer.QrRenderSpec(280));
    parts.add(escpos.alignCenter());
    parts.add(qrBytes);
    parts.add(escpos.alignLeft());
    parts.add(escpos.cut());

    return escpos.concat(parts.toArray(new byte[0][]));
  }

  public byte[] renderQrPng(TicketId ticketId, int sizePx) {
    var ticket = findTicket(ticketId, currentLocale());
    var verifyUrl = urlBuilder.buildUrl(ticket.publicCode());
    return qrPng.render(verifyUrl, new QrRenderer.QrRenderSpec(sizePx));
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
}
