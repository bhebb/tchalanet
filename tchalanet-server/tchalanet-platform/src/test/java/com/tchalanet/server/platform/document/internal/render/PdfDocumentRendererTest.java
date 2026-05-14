package com.tchalanet.server.platform.document.internal.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.GenericDocumentContent;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.document.internal.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.platform.document.internal.qr.zxing.ZxingPngQrRenderer;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfDocumentRendererTest {

  @Test
  void renders_receipt_pdf_with_qr_payload() {
    var renderer = new PdfDocumentRenderer(new ReceiptPdfRenderer(), new ZxingPngQrRenderer());
    var request =
        new DocumentRenderRequest(
            DocumentKind.RECEIPT,
            DocumentFormat.PDF,
            "Ticket",
            ReceiptDocumentContent.ofBodyLines(
                List.of(DocumentLine.of("Line A"), DocumentLine.of("Line B"))),
            List.of(DocumentAsset.qr("qr", "https://t.example/abc", 200)),
            DocumentOptions.defaults(),
            null,
            null);

    var rendered = renderer.render(request);

    assertThat(rendered.contentType()).isEqualTo("application/pdf");
    assertThat(rendered.format()).isEqualTo(DocumentFormat.PDF);
    var bytes = rendered.bytes();
    assertThat(bytes).isNotEmpty();
    // PDF magic
    assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void rejects_unsupported_content_type() {
    var renderer = new PdfDocumentRenderer(new ReceiptPdfRenderer(), new ZxingPngQrRenderer());
    var request =
        new DocumentRenderRequest(
            DocumentKind.GENERIC,
            DocumentFormat.PDF,
            "x",
            GenericDocumentContent.empty(),
            List.of(DocumentAsset.qr("qr", "https://t.example/abc", 200)),
            DocumentOptions.defaults(),
            null,
            null);

    assertThatThrownBy(() -> renderer.render(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ReceiptDocumentContent");
  }
}
