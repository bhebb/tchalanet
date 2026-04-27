package com.tchalanet.server.core.sales.application.print;

import org.springframework.stereotype.Component;

@Component("ticketReceiptFormatterEscPos")
public class EscPosTicketReceiptFormatter extends AbstractTicketReceiptFormatter {

  @Override
  protected String title() {
    return "TCHALANET - TICKET";
  }

  @Override
  protected Labels labels() {
    return new Labels(
        // French ASCII labels for ESC/POS (no accents to keep ASCII-only receipts)
        "TICKET",
        "VERIFIER",
        "TERMINAL",
        "PDV",
        "TIRAGE",
        "TIRAGE LE",
        "ACHETE LE",
        "SCAN QR POUR VERIFIER",
        "JEU",
        "NUMEROS",
        "MISE",
        "GAIN",
        "TOTAL MISE",
        "TOTAL GAIN*");
  }

  @Override
  protected String sanitize(String s) {
    if (s == null) return "";
    // ASCII only
    return s.replaceAll("[^\\x20-\\x7E]", "?");
  }

  @Override
  protected String ellipsis(String s, int max) {
    if (s == null) return "";
    if (s.length() <= max) return s;
    if (max <= 3) return s.substring(0, max);
    return s.substring(0, max - 3) + "...";
  }
}
