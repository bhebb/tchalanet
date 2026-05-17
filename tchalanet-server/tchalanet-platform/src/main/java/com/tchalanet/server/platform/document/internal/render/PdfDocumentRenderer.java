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
import com.tchalanet.server.platform.document.internal.pdf.ReceiptPdfRenderer;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptLine;
import com.tchalanet.server.platform.document.internal.receipt.ReceiptModel;
import com.tchalanet.server.platform.document.internal.util.SafeFilename;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.platform.document.internal.receipt.ReceiptLineStyle.SMALL;
import static com.tchalanet.server.platform.document.internal.receipt.ReceiptLineStyle.WARNING;

@Component
public class PdfDocumentRenderer implements DocumentRenderer {

    private final ReceiptPdfRenderer pdf;
    private final QrRenderer qrPng;

    public PdfDocumentRenderer(
        ReceiptPdfRenderer pdf,
        @Qualifier("qrPngRenderer") QrRenderer qrPng
    ) {
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
                "PDF currently supports only ReceiptDocumentContent, got "
                    + request.content().getClass().getSimpleName());
        }

        var model = toReceiptModel(request.title(), receipt);
        var qrBytes = qrBytes(request);

        byte[] bytes = pdf.render(
            model,
            qrBytes,
            request.options().paperSizeOrDefault()
        );

        return RenderedDocument.of(bytes, DocumentFormat.PDF, fileName(request, "pdf"));
    }

    private byte[] qrBytes(DocumentRenderRequest request) {
        DocumentAsset qr = request.firstAssetOfKind(AssetKind.QR);

        if (qr == null) {
            return null;
        }

        if (qr.bytes() != null && qr.bytes().length > 0) {
            return qr.bytes();
        }

        if (qr.payload() == null || qr.payload().isBlank()) {
            throw new IllegalArgumentException("QR asset must provide bytes or payload");
        }

        int sizePx = qr.sizePx() != null && qr.sizePx() > 0
            ? qr.sizePx()
            : request.options().qrSizePxOrDefault(300);

        return qrPng.render(qr.payload(), new QrRenderer.QrRenderSpec(sizePx));
    }

    private ReceiptModel toReceiptModel(String title, ReceiptDocumentContent content) {
        var safeTitle = title == null || title.isBlank() ? "Ticket Tchalanet" : title;
        List<ReceiptLine> lines = new ArrayList<>();

        for (DocumentLine line : content.headerLines()) {
            lines.add(toReceiptLine(line));
        }

        for (DocumentSection section : content.sections()) {
            if (section.title() != null && !section.title().isBlank()) {
                lines.add(ReceiptLine.bold(section.title()));
            }
            for (DocumentLine line : section.lines()) {
                lines.add(toReceiptLine(line));
            }
        }

        for (DocumentLine line : content.totals()) {
            lines.add(toReceiptLine(line));
        }

        for (DocumentLine line : content.footerLines()) {
            lines.add(toReceiptLine(line));
        }

        return new ReceiptModel(safeTitle, lines);
    }

    private ReceiptLine toReceiptLine(DocumentLine line) {
        return switch (line.style() == null ? LineStyle.NORMAL : line.style()) {
            case TITLE -> ReceiptLine.title(line.text());
            case BOLD -> ReceiptLine.bold(line.text());
            case SMALL -> ReceiptLine.small(line.text());
            case WARNING -> ReceiptLine.warning(line.text());
            case NORMAL -> ReceiptLine.text(line.text());
        };
    }

    private String fileName(DocumentRenderRequest request, String ext) {
        var base = request.metadataValue("filename", request.title());
        return SafeFilename.of(base, request.kind().name().toLowerCase()) + "." + ext;
    }
}
