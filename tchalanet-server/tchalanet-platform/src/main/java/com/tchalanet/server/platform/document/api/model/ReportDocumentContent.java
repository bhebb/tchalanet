package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record ReportDocumentContent(
    List<DocumentLine> summary, List<DocumentTable> tables, List<DocumentLine> footerLines)
    implements DocumentContent {

  public ReportDocumentContent {
    summary = summary == null ? List.of() : List.copyOf(summary);
    tables = tables == null ? List.of() : List.copyOf(tables);
    footerLines = footerLines == null ? List.of() : List.copyOf(footerLines);
  }
}
