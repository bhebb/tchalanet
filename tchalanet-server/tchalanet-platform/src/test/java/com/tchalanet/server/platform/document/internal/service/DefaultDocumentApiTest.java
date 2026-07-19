package com.tchalanet.server.platform.document.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentTemplateKey;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.document.internal.render.DocumentRenderer;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultDocumentApiTest {

  @Test
  void mapsMissingRequestToStableValidationCode() {
    var api = new DefaultDocumentApi(List.of());

    assertThatThrownBy(() -> api.render(null))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "document.request_invalid"));
  }

  @Test
  void mapsRendererFailureToStableServiceCodeWithoutRendererMessage() {
    var api = new DefaultDocumentApi(List.of(failingPdfRenderer()));

    assertThatThrownBy(() -> api.render(receiptRequest()))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error -> {
              var problem = ((ProblemRestException) error).getProblem();
              assertThat(problem.getProperties()).containsEntry("code", "document.render_failed");
              assertThat(problem.getDetail()).doesNotContain("pdfbox internals");
            });
  }

  private DocumentRenderer failingPdfRenderer() {
    return new DocumentRenderer() {
      @Override
      public DocumentFormat format() {
        return DocumentFormat.PDF;
      }

      @Override
      public com.tchalanet.server.platform.document.api.model.RenderedDocument render(
          DocumentRenderRequest request) {
        throw new IllegalStateException("pdfbox internals");
      }
    };
  }

  private DocumentRenderRequest receiptRequest() {
    return new DocumentRenderRequest(
        DocumentTemplateKey.of("sales.ticket.receipt.pdf.v1"),
        DocumentKind.RECEIPT,
        DocumentFormat.PDF,
        "Ticket Tchalanet",
        new ReceiptDocumentContent(List.of(), List.of(), List.of(), List.of(), List.of()),
        List.of(),
        DocumentOptions.receipt80mm(),
        Locale.FRENCH,
        ZoneId.of("America/Port-au-Prince"),
        Map.of());
  }
}
