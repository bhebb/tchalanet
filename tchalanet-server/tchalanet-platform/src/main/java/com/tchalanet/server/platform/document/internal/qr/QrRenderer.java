package com.tchalanet.server.platform.document.internal.qr;

public interface QrRenderer {
  QrFormat format();

  byte[] render(String payload, QrRenderSpec spec);

  enum QrFormat {
    PNG,
    ESC_POS
  }

  record QrRenderSpec(int sizePx) {
    public QrRenderSpec {
      if (sizePx <= 0) sizePx = 280;
    }
  }
}
