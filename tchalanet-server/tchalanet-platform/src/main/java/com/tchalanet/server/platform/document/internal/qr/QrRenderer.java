package com.tchalanet.server.platform.document.internal.qr;

public interface QrRenderer {

    byte[] render(String payload, QrRenderSpec spec);

    record QrRenderSpec(int sizePx) {
        public QrRenderSpec {
            if (sizePx <= 0) {
                throw new IllegalArgumentException("QR size must be positive");
            }
        }
    }
}
