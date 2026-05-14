package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record DocumentTable(List<String> headers, List<List<String>> rows) {
  public DocumentTable {
    headers = headers == null ? List.of() : List.copyOf(headers);
    rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
  }
}
