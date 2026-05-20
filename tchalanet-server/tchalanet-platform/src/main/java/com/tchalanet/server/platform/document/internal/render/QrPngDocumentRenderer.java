package com.tchalanet.server.platform.document.internal.render;

import com.tchalanet.server.platform.document.api.model.AssetKind;
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import com.tchalanet.server.platform.document.internal.util.SafeFilename;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class QrPngDocumentRenderer implements DocumentRenderer {

    private final QrRenderer qrPng;

    public QrPngDocumentRenderer(@Qualifier("qrPngRenderer") QrRenderer qrPng) {
        this.qrPng = qrPng;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.PNG;
    }

    @Override
    public RenderedDocument render(DocumentRenderRequest request) {
        DocumentAsset qr = request.firstAssetOfKind(AssetKind.QR);

        if (qr == null) {
            throw new IllegalArgumentException("PNG render requires a QR asset");
        }

        byte[] bytes;

        if (qr.bytes() != null && qr.bytes().length > 0) {
            bytes = qr.bytes();
        } else {
            if (qr.payload() == null || qr.payload().isBlank()) {
                throw new IllegalArgumentException("QR asset must provide bytes or payload");
            }

            int sizePx = qr.sizePx() != null && qr.sizePx() > 0
                ? qr.sizePx()
                : request.options().qrSizePxOrDefault(280);

            bytes = qrPng.render(qr.payload(), new QrRenderer.QrRenderSpec(sizePx));
        }

        String filename = SafeFilename.of(
            request.metadataValue("filename", "qr"),
            "qr"
        ) + ".png";

        return RenderedDocument.of(bytes, DocumentFormat.PNG, filename);
    }
}
