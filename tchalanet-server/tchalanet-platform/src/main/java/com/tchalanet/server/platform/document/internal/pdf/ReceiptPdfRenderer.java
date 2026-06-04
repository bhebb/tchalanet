package com.tchalanet.server.platform.document.internal.pdf;

import com.tchalanet.server.platform.document.api.model.PaperSize;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptLine;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptLineStyle;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ReceiptPdfRenderer {

    // Thermal receipt (58mm / 80mm): narrow strip, small fonts, tight spacing.
    private static final float THERMAL_MARGIN = 12f;
    private static final float THERMAL_TITLE_SIZE = 13f;
    private static final float THERMAL_NORMAL_SIZE = 9f;
    private static final float THERMAL_SMALL_SIZE = 7f;
    private static final float THERMAL_LINE_GAP = 3f;

    // A4 page: readable layout, standard margins, larger fonts.
    private static final float A4_MARGIN = 40f;
    private static final float A4_HEIGHT_PT = 841.89f;  // ISO A4
    private static final float A4_TITLE_SIZE = 16f;
    private static final float A4_NORMAL_SIZE = 11f;
    private static final float A4_SMALL_SIZE = 9f;
    private static final float A4_LINE_GAP = 5f;

    public byte[] render(ReceiptModel model, byte[] qrPngBytes, PaperSize paperSize) {
        try (var doc = new PDDocument()) {
            boolean isA4 = paperSize == PaperSize.A4;
            float width = paperSize.widthPoints();
            float height = isA4 ? A4_HEIGHT_PT : calculateThermalHeight(model, qrPngBytes);
            var page = new PDPage(new PDRectangle(width, height));
            doc.addPage(page);

            float margin = isA4 ? A4_MARGIN : THERMAL_MARGIN;
            float lineGap = isA4 ? A4_LINE_GAP : THERMAL_LINE_GAP;

            try (var cs = new PDPageContentStream(doc, page)) {
                float y = height - margin;

                // The tenant/outlet branding is already the first thing in
                // model.lines() (canonical branding header). Do NOT also draw
                // model.title() here, or the tenant name prints twice.
                for (ReceiptLine line : model.lines()) {
                    y = drawLine(cs, line, width, y, margin, isA4);
                    y -= lineGap;
                    if (y < margin) break;
                }

                if (qrPngBytes != null && qrPngBytes.length > 0) {
                    y -= isA4 ? 16f : 8f;
                    var qr = PDImageXObject.createFromByteArray(doc, qrPngBytes, "qr");
                    float qrSize = isA4 ? 160f : Math.min(120f, width - (margin * 2));
                    float x = (width - qrSize) / 2f;
                    if (y - qrSize > margin) {
                        cs.drawImage(qr, x, y - qrSize, qrSize, qrSize);
                    }
                }
            }

            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to render receipt PDF", e);
        }
    }

    private float drawLine(
        PDPageContentStream cs,
        ReceiptLine line,
        float pageWidth,
        float y,
        float margin,
        boolean isA4
    ) throws IOException {
        var font = line.style() == ReceiptLineStyle.BOLD
            || line.style() == ReceiptLineStyle.TITLE
            || line.style() == ReceiptLineStyle.WARNING
            ? fontBold()
            : fontRegular();

        float size = isA4
            ? switch (line.style()) {
                case TITLE -> A4_TITLE_SIZE;
                case SMALL -> A4_SMALL_SIZE;
                default -> A4_NORMAL_SIZE;
            }
            : switch (line.style()) {
                case TITLE -> THERMAL_TITLE_SIZE;
                case SMALL -> THERMAL_SMALL_SIZE;
                default -> THERMAL_NORMAL_SIZE;
            };

        if (line.style() == ReceiptLineStyle.TITLE) {
            return drawCenteredText(cs, line.text(), pageWidth, y, font, size) - THERMAL_LINE_GAP;
        }

        drawText(cs, line.text(), margin, y, font, size);
        return y - size;
    }

    private float calculateThermalHeight(ReceiptModel model, byte[] qrPngBytes) {
        float height = THERMAL_MARGIN * 2 + 24f;

        for (ReceiptLine line : model.lines()) {
            height += switch (line.style()) {
                case TITLE -> 16f;
                case BOLD, NORMAL, WARNING -> 12f;
                case SMALL -> 10f;
            };
        }

        if (qrPngBytes != null && qrPngBytes.length > 0) {
            height += 140f;
        }

        return Math.max(height, 220f);
    }

    private float drawCenteredText(
        PDPageContentStream cs,
        String text,
        float pageWidth,
        float y,
        PDType1Font font,
        float fontSize
    ) throws IOException {
        String safe = text == null ? "" : text;
        float textWidth = font.getStringWidth(safe) / 1000f * fontSize;
        float x = Math.max(THERMAL_MARGIN, (pageWidth - textWidth) / 2f);
        drawText(cs, safe, x, y, font, fontSize);
        return y - fontSize - THERMAL_LINE_GAP;
    }

    private void drawText(
        PDPageContentStream cs,
        String text,
        float x,
        float y,
        PDType1Font font,
        float fontSize
    ) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizePdfText(text));
        cs.endText();
    }

    private String sanitizePdfText(String value) {
        if (value == null) return "";
        return value.replace("\n", " ").replace("\r", " ");
    }

    private PDType1Font fontRegular() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDType1Font fontBold() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }
}
