package com.tchalanet.server.common.print.escpos;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class EscPosBuilder {

  public byte[] init() {
    return new byte[] {0x1B, '@'};
  }

  public byte[] alignCenter() {
    return new byte[] {0x1B, 'a', 1};
  }

  public byte[] alignLeft() {
    return new byte[] {0x1B, 'a', 0};
  }

  public byte[] boldOn() {
    return new byte[] {0x1B, 0x45, 0x01}; // ESC E 1
  }

  public byte[] boldOff() {
    return new byte[] {0x1B, 0x45, 0x00}; // ESC E 0
  }

  public byte[] lf() {
    return new byte[] {0x0A};
  }

  public byte[] cut() {
    return new byte[] {0x1D, 'V', 1};
  }

  public byte[] text(String s) {
    return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
  }

  public byte[] concat(byte[]... parts) {
    int size = 0;
    for (var p : parts) if (p != null) size += p.length;

    byte[] out = new byte[size];
    int pos = 0;

    for (var p : parts) {
      if (p == null) continue;
      System.arraycopy(p, 0, out, pos, p.length);
      pos += p.length;
    }
    return out;
  }
}
