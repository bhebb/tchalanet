package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record DocumentSection(String title, List<DocumentLine> lines) {
  public DocumentSection {
    lines = lines == null ? List.of() : List.copyOf(lines);
  }
}
