package com.tchalanet.server.platform.document.internal.render;

import com.tchalanet.server.platform.document.api.model.AssetKind;
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentSection;
import com.tchalanet.server.platform.document.api.model.LineStyle;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.internal.escpos.EscPosBuilder;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class EscPosDocumentRenderer implements DocumentRenderer {

  private final EscPosBuilder escpos;
  private final QrRenderer qrEscPos;

  public EscPosDocumentRenderer(
      EscPosBuilder escpos, @Qualifier("qrEscPosRenderer") QrRenderer qrEscPos) {
    this.escpos = escpos;
    this.qrEscPos = qrEscPos;
  }

  @Override
  public DocumentFormat format() {
    return DocumentFormat.ESC_POS;
  }

  @Override
  public RenderedDocument render(DocumentRenderRequest request) {
    if (!(request.content() instanceof ReceiptDocumentContent receipt)) {
      throw new IllegalArgumentException(
          "ESC_POS format currently supports only ReceiptDocumentContent, got "
              + request.content().getClass().getSimpleName());
    }
    var title =
        request.title() == null || request.title().isBlank() ? "Ticket Tchalanet" : request.title();
    var parts = new ArrayList<byte[]>();
    parts.add(escpos.init());
    parts.add(escpos.alignLeft());

    parts.add(escpos.alignCenter());
    parts.add(escpos.boldOn());
    parts.add(escpos.text(title));
    parts.add(escpos.boldOff());
    parts.add(escpos.lf());
    parts.add(escpos.alignLeft());

    appendLines(parts, receipt.headerLines());
    for (DocumentSection s : receipt.sections()) {
      if (s.title() != null && !s.title().isBlank()) {
        parts.add(escpos.boldOn());
        parts.add(escpos.text(s.title()));
        parts.add(escpos.boldOff());
        parts.add(escpos.lf());
      }
      appendLines(parts, s.lines());
    }
    appendLines(parts, receipt.totals());
    appendLines(parts, receipt.footerLines());

    byte[] qrBytes = qrBytes(request);
    if (qrBytes != null) {
      parts.add(escpos.alignCenter());
      parts.add(qrBytes);
      parts.add(escpos.alignLeft());
    }
    parts.add(escpos.cut());
    byte[] bytes = escpos.concat(parts.toArray(new byte[0][]));
    return RenderedDocument.of(bytes, DocumentFormat.ESC_POS, safeFile(title) + ".bin");
  }

  private void appendLines(List<byte[]> parts, List<DocumentLine> lines) {
    for (DocumentLine l : lines) {
      boolean bold = l.style() == LineStyle.BOLD || l.style() == LineStyle.TITLE;
      parts.add(bold ? escpos.boldOn() : escpos.boldOff());
      parts.add(escpos.text(l.text()));
      parts.add(escpos.boldOff());
      parts.add(escpos.lf());
    }
  }

  private byte[] qrBytes(DocumentRenderRequest request) {
    DocumentAsset qr = request.firstAssetOfKind(AssetKind.QR);
    if (qr == null) return null;
    if (qr.bytes() != null && qr.bytes().length > 0) return qr.bytes();
    if (qr.payload() == null || qr.payload().isBlank()) return null;
    int sizePx =
        qr.sizePx() != null && qr.sizePx() > 0
            ? qr.sizePx()
            : request.options().qrSizePxOrDefault(280);
    return qrEscPos.render(qr.payload(), new QrRenderer.QrRenderSpec(sizePx));
  }

  private String safeFile(String title) {
    return title.replaceAll("\\s+", "_");
  }
}
