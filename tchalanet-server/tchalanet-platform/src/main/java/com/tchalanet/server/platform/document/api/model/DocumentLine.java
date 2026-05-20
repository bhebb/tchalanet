package com.tchalanet.server.platform.document.api.model;

public record DocumentLine(String text, LineStyle style) {
  public DocumentLine {
    if (text == null) text = "";
    if (style == null) style = LineStyle.NORMAL;
  }

  public static DocumentLine of(String text) {
    return new DocumentLine(text, LineStyle.NORMAL);
  }

  public static DocumentLine bold(String text) {
    return new DocumentLine(text, LineStyle.BOLD);
  }
}
