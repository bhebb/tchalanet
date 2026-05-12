package com.tchalanet.server.platform.document.internal.qr.escpos;

import com.tchalanet.server.common.document.qr.QrRenderer;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component("qrEscPosRenderer")
public class EscPosQrRenderer implements QrRenderer {

  private static final byte GS = 0x1D;

  @Override
  public QrFormat format() {
    return QrFormat.ESC_POS;
  }

  @Override
  public byte[] render(String payload, QrRenderSpec spec) {
    var out = new ByteArrayOutput();

    byte[] data = payload.getBytes(StandardCharsets.UTF_8);

    out.add(GS).add((byte) '(').add((byte) 'k').add((byte) 4).add((byte) 0).add((byte) 49).add((byte) 65).add((byte) 50);
    out.add(GS).add((byte) '(').add((byte) 'k').add((byte) 3).add((byte) 0).add((byte) 49).add((byte) 67).add((byte) 6);
    out.add(GS).add((byte) '(').add((byte) 'k').add((byte) 3).add((byte) 0).add((byte) 49).add((byte) 69).add((byte) 49);

    int len = data.length + 3;
    out.add(GS)
        .add((byte) '(')
        .add((byte) 'k')
        .add((byte) (len & 0xFF))
        .add((byte) ((len >> 8) & 0xFF))
        .add((byte) 49)
        .add((byte) 80)
        .add((byte) 48);
    out.add(data);

    out.add(GS).add((byte) '(').add((byte) 'k').add((byte) 3).add((byte) 0).add((byte) 49).add((byte) 81).add((byte) 48);
    out.lf().lf();

    return out.toByteArray();
  }

  static class ByteArrayOutput {
    private byte[] buf = new byte[256];
    private int n = 0;

    ByteArrayOutput add(byte b) {
      ensure(1);
      buf[n++] = b;
      return this;
    }

    ByteArrayOutput add(byte[] arr) {
      ensure(arr.length);
      System.arraycopy(arr, 0, buf, n, arr.length);
      n += arr.length;
      return this;
    }

    ByteArrayOutput lf() {
      return add((byte) '\n');
    }

    byte[] toByteArray() {
      return java.util.Arrays.copyOf(buf, n);
    }

    private void ensure(int k) {
      if (n + k <= buf.length) return;
      buf = java.util.Arrays.copyOf(buf, Math.max(buf.length * 2, n + k));
    }
  }
}
