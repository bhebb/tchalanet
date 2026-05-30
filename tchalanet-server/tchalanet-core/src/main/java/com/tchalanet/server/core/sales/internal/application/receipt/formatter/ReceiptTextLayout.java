package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import org.springframework.stereotype.Component;

@Component
public class ReceiptTextLayout {

    public String separator(TicketReceiptLayoutProfile profile) {
        return "-".repeat(profile.charsPerLine());
    }

    public String truncate(String value, int width) {
        if (value == null) {
            return "";
        }
        var text = value; // ne pas trim ici
        if (text.length() <= width) {
            return text;
        }
        if (width <= 3) {
            return text.substring(0, width);
        }
        return text.substring(0, width - 3) + "...";
    }

    public String center(String value, TicketReceiptLayoutProfile profile) {
        var text = truncate(value, profile.charsPerLine());
        int left = Math.max(0, (profile.charsPerLine() - text.length()) / 2);
        return " ".repeat(left) + text;
    }

    public String rightPad(String value, int width) {
        var text = value == null ? "" : value;
        if (text.length() >= width) {
            return truncate(text, width);
        }
        return text + " ".repeat(width - text.length());
    }

    public String leftPad(String value, int width) {
        var text = value == null ? "" : value;
        if (text.length() >= width) {
            return truncate(text, width);
        }
        return " ".repeat(width - text.length()) + text;
    }

    public String labelValue(String label, String value, TicketReceiptLayoutProfile profile) {
        return truncate(label + ": " + value, profile.charsPerLine());
    }

    public String leftRight(String left, String right, TicketReceiptLayoutProfile profile) {
        var safeLeft = left == null ? "" : left;
        var safeRight = right == null ? "" : right;
        int spaces = profile.charsPerLine() - safeLeft.length() - safeRight.length();

        if (spaces < 1) {
            return truncate(safeLeft + " " + safeRight, profile.charsPerLine());
        }

        return safeLeft + " ".repeat(spaces) + safeRight;
    }
}

