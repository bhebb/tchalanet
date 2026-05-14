package com.tchalanet.server.platform.document.internal.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.GenericDocumentContent;
import com.tchalanet.server.platform.document.internal.qr.zxing.ZxingPngQrRenderer;
import java.util.List;
import org.junit.jupiter.api.Test;

class QrPngDocumentRendererTest {

  @Test
  void renders_qr_png_from_payload() {
    var renderer = new QrPngDocumentRenderer(new ZxingPngQrRenderer());

    var request =
        new DocumentRenderRequest(
            DocumentKind.QR,
            DocumentFormat.PNG,
            "qr",
            GenericDocumentContent.empty(),
            List.of(DocumentAsset.qr("qr", "https://t.example/abc", 200)),
            DocumentOptions.defaults(),
            null,
            null);

    var rendered = renderer.render(request);

    assertThat(rendered.format()).isEqualTo(DocumentFormat.PNG);
    assertThat(rendered.contentType()).isEqualTo("image/png");
    assertThat(rendered.bytes()).isNotEmpty();
    // PNG signature
    assertThat(rendered.bytes()[0]).isEqualTo((byte) 0x89);
    assertThat(rendered.bytes()[1]).isEqualTo((byte) 'P');
    assertThat(rendered.bytes()[2]).isEqualTo((byte) 'N');
    assertThat(rendered.bytes()[3]).isEqualTo((byte) 'G');
  }

  @Test
  void rejects_missing_qr_asset() {
    var renderer = new QrPngDocumentRenderer(new ZxingPngQrRenderer());
    var request =
        new DocumentRenderRequest(
            DocumentKind.QR,
            DocumentFormat.PNG,
            "qr",
            GenericDocumentContent.empty(),
            List.of(),
            DocumentOptions.defaults(),
            null,
            null);

    assertThatThrownBy(() -> renderer.render(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("QR asset");
  }
}
