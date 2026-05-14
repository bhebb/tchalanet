package com.tchalanet.server.platform.document.api.model;

public enum DocumentFormat {
  PDF("application/pdf", "pdf"),
  ESC_POS("application/octet-stream", "bin"),
  PNG("image/png", "png");

  private final String contentType;
  private final String extension;

  DocumentFormat(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public String contentType() {
    return contentType;
  }

  public String extension() {
    return extension;
  }
}
