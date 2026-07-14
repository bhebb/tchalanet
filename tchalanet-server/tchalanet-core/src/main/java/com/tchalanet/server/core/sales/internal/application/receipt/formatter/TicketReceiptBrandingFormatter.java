package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.ReceiptBrandingDisplayMode;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReceiptBrandingFormatter {

  private final ReceiptTextLayout layout;

  public List<TicketReceiptTextLine> headerLines(TicketReceiptView receipt) {
    return headerLines(receipt, ReceiptBrandingDisplayMode.AUTO, ReceiptBrandingDisplayMode.AUTO);
  }

  public List<TicketReceiptTextLine> headerLines(
      TicketReceiptView receipt, TicketReceiptLayoutProfile profile) {
    var lines = headerLines(receipt);
    if (profile == null) {
      return lines;
    }
    var out = new ArrayList<TicketReceiptTextLine>();
    for (var l : lines) {
      out.add(fitBrandingLine(l, profile));
    }
    return List.copyOf(out);
  }

  public List<TicketReceiptTextLine> headerLines(
      TicketReceiptView receipt,
      ReceiptBrandingDisplayMode tenantMode,
      ReceiptBrandingDisplayMode outletMode) {
    var lines = new ArrayList<TicketReceiptTextLine>();
    addBranding(
        lines,
        firstNonBlank(receipt.tenantDisplayName(), "TCHALANET"),
        receipt.tenantReceiptHeader(),
        tenantMode);
    return List.copyOf(lines);
  }

  public List<TicketReceiptTextLine> footerLines(TicketReceiptView receipt) {
    var lines = new ArrayList<TicketReceiptTextLine>();
    addMultiline(lines, receipt.tenantReceiptFooter(), false);
    return List.copyOf(lines);
  }

  public List<TicketReceiptTextLine> footerLines(
      TicketReceiptView receipt, TicketReceiptLayoutProfile profile) {
    var lines = footerLines(receipt);
    if (profile == null) {
      return lines;
    }
    var out = new ArrayList<TicketReceiptTextLine>();
    for (var l : lines) {
      out.add(
          l.style() == null || l.style().name().equals("BOLD")
              ? TicketReceiptTextLine.bold(layout.truncate(l.text(), profile.charsPerLine()))
              : TicketReceiptTextLine.normal(layout.truncate(l.text(), profile.charsPerLine())));
    }
    return List.copyOf(out);
  }

  private TicketReceiptTextLine fitBrandingLine(
      TicketReceiptTextLine line, TicketReceiptLayoutProfile profile) {
    var text = layout.center(line.text(), profile);
    boolean bold =
        line.style() != null
            && line.style()
                == com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineStyle.BOLD;
    return bold ? TicketReceiptTextLine.bold(text) : TicketReceiptTextLine.normal(text);
  }

  private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
    if (value != null && !value.isBlank()) {
      lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
    }
  }

  private void addBranding(
      List<TicketReceiptTextLine> lines,
      String name,
      String header,
      ReceiptBrandingDisplayMode mode) {
    switch (mode == null ? ReceiptBrandingDisplayMode.AUTO : mode) {
      case NAME_ONLY -> add(lines, name, true);
      case HEADER_ONLY -> addMultiline(lines, header, false);
      case NAME_AND_HEADER -> {
        add(lines, name, true);
        addMultiline(lines, header, false);
      }
      case AUTO -> {
        add(lines, name, true);
        if (header != null && !header.isBlank()) {
          addMultiline(lines, header, false);
        }
      }
    }
  }

  private void addMultiline(List<TicketReceiptTextLine> lines, String value, boolean bold) {
    if (value == null || value.isBlank()) {
      return;
    }
    for (var line : value.split("\\R")) {
      add(lines, line, bold);
    }
  }

  private String firstNonBlank(String preferred, String fallback) {
    return preferred == null || preferred.isBlank() ? fallback : preferred;
  }
}
