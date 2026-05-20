package com.tchalanet.server.platform.document.internal.receipt;

public record ReceiptLine(
    String text,
    ReceiptLineStyle style
) {

    public ReceiptLine {
        text = text == null ? "" : text;
        style = style == null ? ReceiptLineStyle.NORMAL : style;
    }

    public static ReceiptLine title(String text) {
        return new ReceiptLine(text, ReceiptLineStyle.TITLE);
    }

    public static ReceiptLine bold(String text) {
        return new ReceiptLine(text, ReceiptLineStyle.BOLD);
    }

    public static ReceiptLine text(String text) {
        return new ReceiptLine(text, ReceiptLineStyle.NORMAL);
    }

    public static ReceiptLine small(String text) {
        return new ReceiptLine(text, ReceiptLineStyle.SMALL);
    }

    public static ReceiptLine warning(String text) {
        return new ReceiptLine(text, ReceiptLineStyle.WARNING);
    }
}
