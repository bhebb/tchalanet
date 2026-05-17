package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record QrDocumentContent(
    List<DocumentLine> headerLines,
    List<DocumentSection> sections,
    List<DocumentLine> totals,
    List<DocumentLine> footerLines)
    implements DocumentContent {

    public QrDocumentContent {
        headerLines = headerLines == null ? List.of() : List.copyOf(headerLines);
        sections = sections == null ? List.of() : List.copyOf(sections);
        totals = totals == null ? List.of() : List.copyOf(totals);
        footerLines = footerLines == null ? List.of() : List.copyOf(footerLines);
    }

    public static QrDocumentContent ofBodyLines(List<DocumentLine> bodyLines) {
        return new QrDocumentContent(bodyLines, List.of(), List.of(), List.of());
    }
}
