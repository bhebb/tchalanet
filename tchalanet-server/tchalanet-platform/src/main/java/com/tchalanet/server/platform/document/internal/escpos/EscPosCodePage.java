package com.tchalanet.server.platform.document.internal.escpos;

import java.nio.charset.Charset;

public enum EscPosCodePage {
    CP437(0, "CP437"),
    CP850(2, "CP850"),
    CP1252(16, "windows-1252");

    private final int escposValue;
    private final String charsetName;

    EscPosCodePage(int escposValue, String charsetName) {
        this.escposValue = escposValue;
        this.charsetName = charsetName;
    }

    public int escposValue() {
        return escposValue;
    }

    public Charset charset() {
        return Charset.forName(charsetName);
    }
}
