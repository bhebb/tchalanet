package com.tchalanet.server.platform.document.internal.service;

import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.internal.escpos.EscPosBuilder;
import com.tchalanet.server.platform.document.internal.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptLine;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentApi implements DocumentApi {

  private final QrRenderer qrPng;
  private final QrRenderer qrEscPos;
  private final ReceiptPdfRenderer pdf;
  private final EscPosBuilder escpos;

  public DefaultDocumentApi(
      @Qualifier("qrPngRenderer") QrRenderer qrPng,
      @Qualifier("qrEscPosRenderer") QrRenderer qrEscPos,
      ReceiptPdfRenderer pdf,
      EscPosBuilder escpos) {
    this.qrPng = qrPng;
    this.qrEscPos = qrEscPos;
    this.pdf = pdf;
    this.escpos = escpos;
  }

  @Override
  public byte[] renderReceiptPdf(String title, List<String> bodyLines, byte[] qrPng) {
    return pdf.render(receiptModel(title, bodyLines), qrPng);
  }

  @Override
  public byte[] renderReceiptEscPos(String title, List<String> bodyLines, byte[] qrEscPos) {
    var model = receiptModel(title, bodyLines);
    var parts = new ArrayList<byte[]>();
    parts.add(escpos.init());
    parts.add(escpos.alignLeft());

    parts.add(escpos.alignCenter());
    parts.add(escpos.boldOn());
    parts.add(escpos.text(model.title()));
    parts.add(escpos.boldOff());
    parts.add(escpos.lf());
    parts.add(escpos.alignLeft());

    for (var line : model.lines()) {
      for (var span : line.spans()) {
        parts.add(span.bold() ? escpos.boldOn() : escpos.boldOff());
        parts.add(escpos.text(span.text()));
      }
      parts.add(escpos.boldOff());
      parts.add(escpos.lf());
    }

    parts.add(escpos.alignCenter());
    parts.add(qrEscPos);
    parts.add(escpos.alignLeft());
    parts.add(escpos.cut());
    return escpos.concat(parts.toArray(new byte[0][]));
  }

  @Override
  public byte[] renderQrPng(String payload, int sizePx) {
    return qrPng.render(payload, new QrRenderer.QrRenderSpec(sizePx));
  }

  @Override
  public byte[] renderQrEscPos(String payload, int sizePx) {
    return qrEscPos.render(payload, new QrRenderer.QrRenderSpec(sizePx));
  }

  private ReceiptModel receiptModel(String title, List<String> bodyLines) {
    var safeTitle = title == null || title.isBlank() ? "Ticket Tchalanet" : title;
    var lines = bodyLines == null ? List.<String>of() : bodyLines;
    return new ReceiptModel(safeTitle, lines.stream().map(ReceiptLine::text).toList());
  }
}
