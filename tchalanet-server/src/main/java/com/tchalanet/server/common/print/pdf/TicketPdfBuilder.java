package com.tchalanet.server.common.print.pdf;

import com.tchalanet.server.common.print.receipt.ReceiptModel;
import com.tchalanet.server.common.print.receipt.ReceiptSpan;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class TicketPdfBuilder {

  public byte[] buildReceiptPdf(ReceiptModel model, byte[] qrPng) {
    float width = 226; // ~80mm
    float height = 600; // simple; tu peux ajuster dynamiquement plus tard
    PDRectangle pageSize = new PDRectangle(width, height);

    try (var doc = new PDDocument();
        var out = new ByteArrayOutputStream()) {
      var page = new PDPage(pageSize);
      doc.addPage(page);

      PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrPng, "qr.png");

      // Titre
      PDFont titleBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      // Corps: monospace pour alignement des colonnes
      PDFont bodyNormal = new PDType1Font(Standard14Fonts.FontName.COURIER);
      PDFont bodyBold = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        float y = height - 20;

        // Title
        cs.beginText();
        cs.setFont(titleBold, 12f);
        cs.newLineAtOffset(10, y);
        cs.showText(safe(model.title()));
        cs.endText();
        y -= 20;

        // Lines with spans (bold/normal)
        float fontSize = 9f;
        for (var line : model.lines()) {
          float x = 10;

          for (ReceiptSpan sp : line.spans()) {
            var f = sp.bold() ? bodyBold : bodyNormal;
            String txt = safe(sp.text());

            cs.beginText();
            cs.setFont(f, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(txt);
            cs.endText();

            x += textWidth(f, fontSize, txt);
          }

          y -= 12;
          if (y < 20) break;
        }

        // QR centered at bottom
        y -= 10;
        float qrSize = 140;
        cs.drawImage(qr, (width - qrSize) / 2, y - qrSize, qrSize, qrSize);
      }

      doc.save(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build ticket PDF", e);
    }
  }

  private float textWidth(PDFont font, float fontSize, String text) throws java.io.IOException {
    return (font.getStringWidth(text) / 1000f) * fontSize;
  }

  private String safe(String s) {
    if (s == null) return "";
    return s.replace("\t", "  ");
  }
}
