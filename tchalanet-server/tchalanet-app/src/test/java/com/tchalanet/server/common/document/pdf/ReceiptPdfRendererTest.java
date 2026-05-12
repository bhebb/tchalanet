package com.tchalanet.server.common.document.pdf;

import com.tchalanet.server.common.document.qr.QrRenderer;
import com.tchalanet.server.common.document.qr.zxing.ZxingPngQrRenderer;
import com.tchalanet.server.common.document.receipt.ReceiptLine;
import com.tchalanet.server.common.document.receipt.ReceiptModel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptPdfRendererTest {

  @Test
  void rendersSimpleReceiptModel() {
    var qr = new ZxingPngQrRenderer().render("ticket-public-code", new QrRenderer.QrRenderSpec(120));
    var pdf = new ReceiptPdfRenderer().render(
        new ReceiptModel("Tchalanet", List.of(ReceiptLine.text("Ticket TK-001"))),
        qr
    );

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }
}
