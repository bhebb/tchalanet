package com.tchalanet.server.core.sales.api.model.receipt;

public record TicketReceiptTextLine(
    String text,
    TicketReceiptLineStyle style
) {
    public TicketReceiptTextLine {
        text = text == null ? "" : text;
        style = style == null ? TicketReceiptLineStyle.NORMAL : style;
    }

    public static TicketReceiptTextLine normal(String text) {
        return new TicketReceiptTextLine(text, TicketReceiptLineStyle.NORMAL);
    }

    public static TicketReceiptTextLine bold(String text) {
        return new TicketReceiptTextLine(text, TicketReceiptLineStyle.BOLD);
    }

    public static TicketReceiptTextLine small(String text) {
        return new TicketReceiptTextLine(text, TicketReceiptLineStyle.SMALL);
    }
}
