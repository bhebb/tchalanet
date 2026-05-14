package com.tchalanet.server.platform.document.api.model;

public record RenderedDocument(byte[] bytes, String contentType, String filename, DocumentFormat format) {

  public static RenderedDocument of(byte[] bytes, DocumentFormat format, String filename) {
    return new RenderedDocument(bytes, format.contentType(), filename, format);
  }
}
