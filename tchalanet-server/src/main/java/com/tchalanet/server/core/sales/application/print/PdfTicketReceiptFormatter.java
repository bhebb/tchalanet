package com.tchalanet.server.core.sales.application.print;

import org.springframework.stereotype.Component;

@Component("ticketReceiptFormatterPdf")
public class PdfTicketReceiptFormatter extends AbstractTicketReceiptFormatter {

  @Override
  protected String title() {
    return "TCHALANET - TICKET";
  }

  @Override
  protected Labels labels() {
    return new Labels(
        "Ticket",
        "Verifier",
        "Terminal",
        "PDV",
        "Tirage",
        "Tirage le",
        "Achat le",
        "Scanner pour verifier",
        "JEU",
        "NUMEROS",
        "MISE",
        "GAIN",
        "TOTAL MISE",
        "TOTAL GAIN POT.");
  }

  @Override
  protected String sanitize(String s) {
    if (s == null) return "";
    return s.replace("\t", "  ");
  }

  @Override
  protected String ellipsis(String s, int max) {
    if (s == null) return "";
    if (max <= 1) return s.substring(0, 1);
    return s.length() <= max ? s : s.substring(0, max - 1) + "…";
  }
}

