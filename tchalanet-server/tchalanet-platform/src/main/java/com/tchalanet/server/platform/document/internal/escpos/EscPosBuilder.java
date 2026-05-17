package com.tchalanet.server.platform.document.internal.escpos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.stereotype.Component;

@Component
public class EscPosBuilder {

    public byte[] init() {
        return new byte[] {0x1B, 0x40};
    }

    public byte[] selectCodePage(EscPosCodePage codePage) {
        return new byte[] {0x1B, 0x74, (byte) codePage.escposValue()};
    }

    public byte[] alignLeft() {
        return new byte[] {0x1B, 0x61, 0x00};
    }

    public byte[] alignCenter() {
        return new byte[] {0x1B, 0x61, 0x01};
    }

    public byte[] boldOn() {
        return new byte[] {0x1B, 0x45, 0x01};
    }

    public byte[] boldOff() {
        return new byte[] {0x1B, 0x45, 0x00};
    }

    public byte[] text(String value, EscPosCodePage codePage) {
        String safe = value == null ? "" : value;
        return safe.getBytes(codePage.charset());
    }

    public byte[] lf() {
        return new byte[] {0x0A};
    }

    public byte[] feed(int lines) {
        return new byte[] {0x1B, 0x64, (byte) Math.max(0, Math.min(lines, 10))};
    }

    public byte[] cut() {
        return new byte[] {0x1D, 0x56, 0x41, 0x10};
    }

    public byte[] concat(byte[]... chunks) {
        try {
            var out = new ByteArrayOutputStream();
            for (byte[] chunk : Arrays.stream(chunks).filter(c -> c != null && c.length > 0).toList()) {
                out.write(chunk);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to concatenate ESC/POS bytes", e);
        }
    }
}
