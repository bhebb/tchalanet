package com.tchalanet.server.platform.document.internal.render;

import com.tchalanet.server.platform.document.api.model.AssetKind;
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentSection;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.internal.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptLine;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentRenderer implements DocumentRenderer {

  private final ReceiptPdfRenderer pdf;
  private final QrRenderer qrPng;

  public PdfDocumentRenderer(
      ReceiptPdfRenderer pdf, @Qualifier("qrPngRenderer") QrRenderer qrPng) {
    this.pdf = pdf;
    this.qrPng = qrPng;
  }

  @Override
  public DocumentFormat format() {
    return DocumentFormat.PDF;
  }

  @Override
  public RenderedDocument render(DocumentRenderRequest request) {
    if (!(request.content() instanceof ReceiptDocumentContent receipt)) {
      throw new IllegalArgumentException(
          "PDF format currently supports only ReceiptDocumentContent, got "
              + request.content().getClass().getSimpleName());
    }
    var model = toReceiptModel(request.title(), receipt);
    var qrBytes = qrBytes(request);
    byte[] bytes = pdf.render(model, qrBytes);
    return RenderedDocument.of(bytes, DocumentFormat.PDF, fileName(request, "pdf"));
  }

  private byte[] qrBytes(DocumentRenderRequest request) {
    DocumentAsset qr = request.firstAssetOfKind(AssetKind.QR);
    if (qr == null) {
      throw new IllegalArgumentException("PDF receipt requires a QR asset");
    }
    if (qr.bytes() != null && qr.bytes().length > 0) {
      return qr.bytes();
    }
    if (qr.payload() == null || qr.payload().isBlank()) {
      throw new IllegalArgumentException("QR asset must provide bytes or payload");
    }
    int sizePx =
        qr.sizePx() != null && qr.sizePx() > 0
            ? qr.sizePx()
            : request.options().qrSizePxOrDefault(300);
    return qrPng.render(qr.payload(), new QrRenderer.QrRenderSpec(sizePx));
  }

  private ReceiptModel toReceiptModel(String title, ReceiptDocumentContent content) {
    var safeTitle = title == null || title.isBlank() ? "Ticket Tchalanet" : title;
    List<ReceiptLine> lines = new ArrayList<>();
    for (DocumentLine l : content.headerLines()) lines.add(ReceiptLine.text(l.text()));
    for (DocumentSection s : content.sections()) {
      if (s.title() != null && !s.title().isBlank()) lines.add(ReceiptLine.text(s.title()));
      for (DocumentLine l : s.lines()) lines.add(ReceiptLine.text(l.text()));
    }
    for (DocumentLine l : content.totals()) lines.add(ReceiptLine.text(l.text()));
    for (DocumentLine l : content.footerLines()) lines.add(ReceiptLine.text(l.text()));
    return new ReceiptModel(safeTitle, lines);
  }

  private String fileName(DocumentRenderRequest request, String ext) {
    return (request.title() == null || request.title().isBlank()
            ? request.kind().name().toLowerCase()
            : request.title().replaceAll("\\s+", "_"))
        + "."
        + ext;
  }
}
