package com.tchalanet.server.platform.document.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.GenericDocumentContent;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.internal.render.DocumentRenderer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultDocumentApiTest {

  private DocumentRenderer rendererFor(DocumentFormat format, byte[] payload) {
    return new DocumentRenderer() {
      @Override
      public DocumentFormat format() {
        return format;
      }

      @Override
      public RenderedDocument render(DocumentRenderRequest request) {
        return RenderedDocument.of(payload, format, "fake." + format.extension());
      }
    };
  }

  private DocumentRenderRequest sample(DocumentFormat format) {
    return new DocumentRenderRequest(
        DocumentKind.GENERIC,
        format,
        "t",
        GenericDocumentContent.empty(),
        List.of(),
        DocumentOptions.defaults(),
        null,
        null);
  }

  @Nested
  @DisplayName("dispatch")
  class Dispatch {
    @Test
    void delegates_to_renderer_matching_format() {
      var api =
          new DefaultDocumentApi(
              List.of(
                  rendererFor(DocumentFormat.PDF, new byte[] {1}),
                  rendererFor(DocumentFormat.PNG, new byte[] {2})));

      assertThat(api.render(sample(DocumentFormat.PDF)).bytes()).containsExactly(1);
      assertThat(api.render(sample(DocumentFormat.PNG)).bytes()).containsExactly(2);
    }

    @Test
    void rejects_null_request() {
      var api = new DefaultDocumentApi(List.of(rendererFor(DocumentFormat.PDF, new byte[] {1})));
      assertThatThrownBy(() -> api.render(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_missing_renderer_for_format() {
      var api = new DefaultDocumentApi(List.of(rendererFor(DocumentFormat.PDF, new byte[] {1})));
      assertThatThrownBy(() -> api.render(sample(DocumentFormat.ESC_POS)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No renderer");
    }

    @Test
    void rejects_duplicate_renderers_for_same_format() {
      assertThatThrownBy(
              () ->
                  new DefaultDocumentApi(
                      List.of(
                          rendererFor(DocumentFormat.PDF, new byte[] {1}),
                          rendererFor(DocumentFormat.PDF, new byte[] {2}))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Duplicate");
    }
  }
}
