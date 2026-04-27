package com.tchalanet.server.common.print.receipt;

import java.util.ArrayList;
import java.util.List;

public final class ReceiptLine {
  private final List<ReceiptSpan> spans;

  public ReceiptLine(List<ReceiptSpan> spans) {
    this.spans = List.copyOf(spans);
  }

  public List<ReceiptSpan> spans() {
    return spans;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ReceiptLine text(String s) {
    return new ReceiptLine(List.of(new ReceiptSpan(s, false)));
  }

  public static final class Builder {
    private final List<ReceiptSpan> spans = new ArrayList<>();

    public Builder normal(String text) {
      spans.add(new ReceiptSpan(text, false));
      return this;
    }

    public Builder bold(String text) {
      spans.add(new ReceiptSpan(text, true));
      return this;
    }

    public ReceiptLine build() {
      return new ReceiptLine(spans);
    }
  }
}
