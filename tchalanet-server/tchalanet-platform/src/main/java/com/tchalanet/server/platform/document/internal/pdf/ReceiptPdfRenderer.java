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

    private static final float MARGIN = 12f;
    private static final float TITLE_FONT_SIZE = 13f;
    private static final float NORMAL_FONT_SIZE = 9f;
    private static final float SMALL_FONT_SIZE = 7f;
    private static final float LINE_GAP = 3f;

    public byte[] render(ReceiptModel model, byte[] qrPngBytes, PaperSize paperSize) {
        try (var doc = new PDDocument()) {
            float width = paperSize.widthPoints();
            float height = calculateHeight(model, qrPngBytes);
            var page = new PDPage(new PDRectangle(width, height));
            doc.addPage(page);

            try (var cs = new PDPageContentStream(doc, page)) {
                float y = height - MARGIN;

                y = drawCenteredText(cs, model.title(), width, y, fontBold(), TITLE_FONT_SIZE);
                y -= 8f;

                for (ReceiptLine line : model.lines()) {
                    y = drawLine(cs, line, width, y);
                }

                if (qrPngBytes != null && qrPngBytes.length > 0) {
                    y -= 8f;
                    var qr = PDImageXObject.createFromByteArray(doc, qrPngBytes, "qr");
                    float qrSize = Math.min(120f, width - (MARGIN * 2));
                    float x = (width - qrSize) / 2f;
                    cs.drawImage(qr, x, Math.max(MARGIN, y - qrSize), qrSize, qrSize);
                }
            }

            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to render receipt PDF", e);
        }
    }

    private float calculateHeight(ReceiptModel model, byte[] qrPngBytes) {
        float height = MARGIN * 2 + 24f;

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

    private float drawLine(
        PDPageContentStream cs,
        ReceiptLine line,
        float pageWidth,
        float y
    ) throws IOException {
        var font = line.style() == ReceiptLineStyle.BOLD
            || line.style() == ReceiptLineStyle.TITLE
            || line.style() == ReceiptLineStyle.WARNING
            ? fontBold()
            : fontRegular();

        float size = switch (line.style()) {
            case TITLE -> TITLE_FONT_SIZE;
            case SMALL -> SMALL_FONT_SIZE;
            default -> NORMAL_FONT_SIZE;
        };

        if (line.style() == ReceiptLineStyle.TITLE) {
            return drawCenteredText(cs, line.text(), pageWidth, y, font, size) - LINE_GAP;
        }

        drawText(cs, line.text(), MARGIN, y, font, size);
        return y - size - LINE_GAP;
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
        float x = Math.max(MARGIN, (pageWidth - textWidth) / 2f);
        drawText(cs, safe, x, y, font, fontSize);
        return y - fontSize - LINE_GAP;
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
