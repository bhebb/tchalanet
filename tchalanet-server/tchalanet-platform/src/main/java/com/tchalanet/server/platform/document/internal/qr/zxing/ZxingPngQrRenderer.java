package com.tchalanet.server.platform.document.internal.qr.zxing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component("qrPngRenderer")
public class ZxingPngQrRenderer implements QrRenderer {

    @Override
    public byte[] render(String payload, QrRenderSpec spec) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("QR payload is required");
        }
        if (spec == null) {
            throw new IllegalArgumentException("QR render spec is required");
        }

        try {
            var writer = new QRCodeWriter();
            var matrix =
                writer.encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    spec.sizePx(),
                    spec.sizePx(),
                    Map.of(EncodeHintType.MARGIN, 1));

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render QR PNG", e);
        }
    }
}
