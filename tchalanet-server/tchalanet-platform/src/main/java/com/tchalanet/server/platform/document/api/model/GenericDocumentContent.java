package com.tchalanet.server.platform.document.api.model;

import java.util.List;

public record GenericDocumentContent(List<DocumentSection> sections) implements DocumentContent {

  public GenericDocumentContent {
    sections = sections == null ? List.of() : List.copyOf(sections);
  }

  public static GenericDocumentContent empty() {
    return new GenericDocumentContent(List.of());
  }
}
