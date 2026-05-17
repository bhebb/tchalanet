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
import com.tchalanet.server.platform.document.internal.escpos.EscPosCodePage;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import com.tchalanet.server.platform.document.internal.util.SafeFilename;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class EscPosDocumentRenderer implements DocumentRenderer {

    private final EscPosBuilder escpos;
    private final QrRenderer qrEscPos;

    public EscPosDocumentRenderer(
        EscPosBuilder escpos,
        @Qualifier("qrEscPosNativeRenderer") QrRenderer qrEscPos
    ) {
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
                "ESC_POS currently supports only ReceiptDocumentContent, got "
                    + request.content().getClass().getSimpleName());
        }

        var title = request.title() == null || request.title().isBlank()
            ? "Ticket Tchalanet"
            : request.title();

        var codePage = request.options().escPosCodePageOrDefault();
        var parts = new ArrayList<byte[]>();

        parts.add(escpos.init());
        parts.add(escpos.selectCodePage(codePage));
        parts.add(escpos.alignCenter());
        parts.add(escpos.boldOn());
        parts.add(text(title, codePage));
        parts.add(escpos.boldOff());
        parts.add(escpos.lf());
        parts.add(escpos.alignLeft());

        appendLines(parts, receipt.headerLines(), codePage);

        for (DocumentSection section : receipt.sections()) {
            if (section.title() != null && !section.title().isBlank()) {
                parts.add(escpos.boldOn());
                parts.add(text(section.title(), codePage));
                parts.add(escpos.boldOff());
                parts.add(escpos.lf());
            }

            appendLines(parts, section.lines(), codePage);
        }

        appendLines(parts, receipt.totals(), codePage);
        appendLines(parts, receipt.footerLines(), codePage);

        var qrBytes = qrBytes(request);
        if (qrBytes != null && qrBytes.length > 0) {
            parts.add(escpos.lf());
            parts.add(escpos.alignCenter());
            parts.add(qrBytes);
            parts.add(escpos.alignLeft());
            parts.add(escpos.lf());
        }

        parts.add(escpos.feed(3));
        parts.add(escpos.cut());

        byte[] bytes = escpos.concat(parts.toArray(new byte[0][]));

        return RenderedDocument.of(bytes, DocumentFormat.ESC_POS, fileName(request, "bin"));
    }

    private void appendLines(
        List<byte[]> parts,
        List<DocumentLine> lines,
        EscPosCodePage codePage
    ) {
        for (DocumentLine line : lines) {
            var style = line.style() == null ? LineStyle.NORMAL : line.style();

            boolean bold = style == LineStyle.BOLD
                || style == LineStyle.TITLE
                || style == LineStyle.WARNING;

            if (bold) {
                parts.add(escpos.boldOn());
            } else {
                parts.add(escpos.boldOff());
            }

            parts.add(text(line.text(), codePage));
            parts.add(escpos.boldOff());
            parts.add(escpos.lf());
        }
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
            return null;
        }

        int sizePx = qr.sizePx() != null && qr.sizePx() > 0
            ? qr.sizePx()
            : request.options().qrSizePxOrDefault(280);

        return qrEscPos.render(qr.payload(), new QrRenderer.QrRenderSpec(sizePx));
    }

    private byte[] text(String value, EscPosCodePage codePage) {
        return escpos.text(value, codePage);
    }

    private String fileName(DocumentRenderRequest request, String ext) {
        var base = request.metadataValue("filename", request.title());
        return SafeFilename.of(base, request.kind().name().toLowerCase()) + "." + ext;
    }
}
