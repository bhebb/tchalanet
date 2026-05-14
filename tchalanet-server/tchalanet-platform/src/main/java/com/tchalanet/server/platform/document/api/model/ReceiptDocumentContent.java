package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record ReceiptDocumentContent(
    List<DocumentLine> headerLines,
    List<DocumentSection> sections,
    List<DocumentLine> totals,
    List<DocumentLine> footerLines)
    implements DocumentContent {

  public ReceiptDocumentContent {
    headerLines = headerLines == null ? List.of() : List.copyOf(headerLines);
    sections = sections == null ? List.of() : List.copyOf(sections);
    totals = totals == null ? List.of() : List.copyOf(totals);
    footerLines = footerLines == null ? List.of() : List.copyOf(footerLines);
  }

  public static ReceiptDocumentContent ofBodyLines(List<DocumentLine> bodyLines) {
    return new ReceiptDocumentContent(bodyLines, List.of(), List.of(), List.of());
  }
}
