package com.tchalanet.server.platform.document.internal.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentTemplateKey;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.document.internal.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfDocumentRendererTest {

  @Test
  void rendersPostQrLinesAfterQrSection() throws Exception {
    var renderer = new PdfDocumentRenderer(new ReceiptPdfRenderer(), emptyQrRenderer());
    var request =
        new DocumentRenderRequest(
            DocumentTemplateKey.of("sales.ticket.receipt.pdf.v1"),
            DocumentKind.RECEIPT,
            DocumentFormat.PDF,
            "Ticket Tchalanet",
            new ReceiptDocumentContent(
                List.of(DocumentLine.bold("Header")),
                List.of(),
                List.of(DocumentLine.bold("TOTAL: 1.00")),
                List.of(DocumentLine.of("Scannez le code QR")),
                List.of(DocumentLine.of("QA FOOTER CONFIGURE - Code public + QR"))),
            List.of(),
            DocumentOptions.receipt80mm(),
            Locale.FRENCH,
            ZoneId.of("America/Port-au-Prince"),
            Map.of("filename", "receipt-test"));

    var rendered = renderer.render(request);

    assertThat(extractText(rendered.bytes()))
        .contains("Scannez le code QR")
        .contains("QA FOOTER CONFIGURE - Code public + QR");
  }

  private QrRenderer emptyQrRenderer() {
    return (_payload, _spec) -> new byte[0];
  }

  private String extractText(byte[] pdfBytes) throws IOException {
    try (var document = Loader.loadPDF(pdfBytes)) {
      return new PDFTextStripper().getText(document);
    }
  }
}
