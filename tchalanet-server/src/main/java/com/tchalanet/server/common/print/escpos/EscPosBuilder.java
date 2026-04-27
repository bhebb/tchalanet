package com.tchalanet.server.common.print.escpos;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class EscPosBuilder {

    public byte[] init() {
        return new byte[]{0x1B, 0x40}; // ESC @
    }

    public byte[] alignCenter() {
        return new byte[]{0x1B, 0x61, 0x01};
    }

    public byte[] alignLeft() {
        return new byte[]{0x1B, 0x61, 0x00};
    }

    public byte[] boldOn() {
        return new byte[]{0x1B, 0x45, 0x01}; // ESC E 1
    }

    public byte[] boldOff() {
        return new byte[]{0x1B, 0x45, 0x00}; // ESC E 0
    }

    public byte[] lf() {
        return new byte[]{0x0A};
    }

    public byte[] feed(int n) {
        n = Math.max(0, Math.min(20, n));
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) out[i] = 0x0A;
        return out;
    }

    public byte[] cut() {
        return new byte[]{0x1D, 0x56, 0x00}; // GS V 0 (full cut)
    }

    public byte[] text(String s) {
        // MVP: ASCII-only to avoid printer encoding issues on BT
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.US_ASCII);
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
