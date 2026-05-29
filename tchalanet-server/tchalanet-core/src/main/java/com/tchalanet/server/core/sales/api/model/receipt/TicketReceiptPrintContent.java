package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.ZoneId;

public record TicketReceiptPrintContent(
    String title,
    List<TicketReceiptTextLine> headerLines,
    List<TicketReceiptSectionContent> sections,
    List<TicketReceiptTextLine> totals,
    List<TicketReceiptTextLine> footerLines,
    TicketReceiptQrView qr,
    String filenameBase,
    Locale locale,
    ZoneId timezone,
    Map<String, String> metadata
) {
    public TicketReceiptPrintContent {
        Objects.requireNonNull(title, "title is required");
        headerLines = headerLines == null ? List.of() : List.copyOf(headerLines);
        sections = sections == null ? List.of() : List.copyOf(sections);
        totals = totals == null ? List.of() : List.copyOf(totals);
        footerLines = footerLines == null ? List.of() : List.copyOf(footerLines);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public List<String> bodyLines() {
        var lines = new java.util.ArrayList<String>();
        headerLines.forEach(line -> lines.add(line.text()));
        for (var section : sections) {
            if (section.title() != null && !section.title().isBlank()) {
                lines.add(section.title());
            }
            section.lines().forEach(line -> lines.add(line.text()));
        }
        totals.forEach(line -> lines.add(line.text()));
        footerLines.forEach(line -> lines.add(line.text()));
        return List.copyOf(lines);
    }

    public String qrPayload() {
        return qr == null ? null : qr.payload();
    }
}
