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
import com.tchalanet.server.platform.document.internal.escpos.EscPosBuilder;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import java.util.List;
import org.junit.jupiter.api.Test;

class EscPosDocumentRendererTest {

  private final QrRenderer stubQr =
      new QrRenderer() {
        @Override
        public QrFormat format() {
          return QrFormat.ESC_POS;
        }

        @Override
        public byte[] render(String payload, QrRenderSpec spec) {
          return new byte[] {0x42};
        }
      };

  @Test
  void renders_receipt_with_init_and_cut() {
    var renderer = new EscPosDocumentRenderer(new EscPosBuilder(), stubQr);
    var request =
        new DocumentRenderRequest(
            DocumentKind.RECEIPT,
            DocumentFormat.ESC_POS,
            "Hello",
            ReceiptDocumentContent.ofBodyLines(List.of(DocumentLine.of("line 1"))),
            List.of(DocumentAsset.qr("qr", "https://t.example/a", 200)),
            DocumentOptions.defaults(),
            null,
            null);

    var rendered = renderer.render(request);

    assertThat(rendered.contentType()).isEqualTo("application/octet-stream");
    assertThat(rendered.format()).isEqualTo(DocumentFormat.ESC_POS);
    var bytes = rendered.bytes();
    assertThat(bytes).isNotEmpty();
    assertThat(bytes[0]).isEqualTo((byte) 0x1B);
    assertThat(bytes[1]).isEqualTo((byte) 0x40);
    // GS V 0 cut at the end
    int n = bytes.length;
    assertThat(bytes[n - 3]).isEqualTo((byte) 0x1D);
    assertThat(bytes[n - 2]).isEqualTo((byte) 0x56);
    assertThat(bytes[n - 1]).isEqualTo((byte) 0x00);
  }

  @Test
  void rejects_unsupported_content_type() {
    var renderer = new EscPosDocumentRenderer(new EscPosBuilder(), stubQr);
    var request =
        new DocumentRenderRequest(
            DocumentKind.GENERIC,
            DocumentFormat.ESC_POS,
            "x",
            GenericDocumentContent.empty(),
            List.of(),
            DocumentOptions.defaults(),
            null,
            null);

    assertThatThrownBy(() -> renderer.render(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ReceiptDocumentContent");
  }
}
