package com.tchalanet.server.features.receipt.app;

import com.tchalanet.server.core.sales.api.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.print.TicketReceiptFormatter;
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.GenericDocumentContent;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptDocumentRequestFactory {

  private static final int DEFAULT_QR_PDF_SIZE = 300;
  private static final int DEFAULT_QR_ESCPOS_SIZE = 280;

  private final TicketReceiptFormatter pdfFormatter;
  private final TicketReceiptFormatter escPosFormatter;

  public TicketReceiptDocumentRequestFactory(
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter pdfFormatter,
      @Qualifier("ticketReceiptFormatterEscPos") TicketReceiptFormatter escPosFormatter) {
    this.pdfFormatter = pdfFormatter;
    this.escPosFormatter = escPosFormatter;
  }

  public DocumentRenderRequest receiptRequest(
      TicketPrintView ticket, String verifyUrl, DocumentFormat format, Locale locale) {
    var formatter = format == DocumentFormat.ESC_POS ? escPosFormatter : pdfFormatter;
    var text = receiptText(formatter.formatText(ticket, verifyUrl));
    int qrSize = format == DocumentFormat.ESC_POS ? DEFAULT_QR_ESCPOS_SIZE : DEFAULT_QR_PDF_SIZE;
    var bodyLines = text.bodyLines().stream().map(DocumentLine::of).toList();
    return new DocumentRenderRequest(
        DocumentKind.RECEIPT,
        format,
        text.title(),
        ReceiptDocumentContent.ofBodyLines(bodyLines),
        List.of(DocumentAsset.qr("ticket-qr", verifyUrl, qrSize)),
        DocumentOptions.defaults(),
        locale,
        java.util.Map.of());
  }

  public DocumentRenderRequest qrPngRequest(String verifyUrl, int sizePx, Locale locale) {
    return new DocumentRenderRequest(
        DocumentKind.QR,
        DocumentFormat.PNG,
        "qr",
        GenericDocumentContent.empty(),
        List.of(DocumentAsset.qr("qr", verifyUrl, sizePx)),
        DocumentOptions.defaults(),
        locale,
        java.util.Map.of());
  }

  private ReceiptText receiptText(String text) {
    var lines = text == null ? List.<String>of() : text.lines().toList();
    var title = lines.isEmpty() ? "Ticket Tchalanet" : lines.get(0);
    var body = lines.stream().skip(1).toList();
    return new ReceiptText(title, body);
  }

  private record ReceiptText(String title, List<String> bodyLines) {}
}
