package com.tchalanet.server.platform.document.internal.qr.escpos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.tchalanet.server.platform.document.internal.qr.QrRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("qrEscPosNativeRenderer")
public class QrEscPosNativeRenderer implements QrRenderer {

    @Override
    public byte[] render(String payload, QrRenderSpec spec) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("QR payload is required");
        }

        try {
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            int storeLen = data.length + 3;
            int pL = storeLen % 256;
            int pH = storeLen / 256;

            var out = new ByteArrayOutputStream();

            // Model 2
            out.write(new byte[] {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});

            // Size: 1-16
            int moduleSize = escPosModuleSize(spec.sizePx());
            out.write(new byte[] {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) moduleSize});

            // Error correction M
            out.write(new byte[] {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31});

            // Store data
            out.write(new byte[] {0x1D, 0x28, 0x6B, (byte) pL, (byte) pH, 0x31, 0x50, 0x30});
            out.write(data);

            // Print
            out.write(new byte[] {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});

            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to render native ESC/POS QR", e);
        }
    }

    private int escPosModuleSize(int sizePx) {
        if (sizePx >= 320) return 8;
        if (sizePx >= 280) return 7;
        if (sizePx >= 240) return 6;
        if (sizePx >= 200) return 5;
        return 4;
    }
}
