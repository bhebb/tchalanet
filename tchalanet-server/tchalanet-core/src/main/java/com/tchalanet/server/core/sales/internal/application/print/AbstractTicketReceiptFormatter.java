package com.tchalanet.server.core.sales.internal.application.print;

import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintView;

public abstract class AbstractTicketReceiptFormatter implements TicketReceiptFormatter {

  protected abstract String title();

  protected abstract Labels labels();

  protected abstract String sanitize(String s);

  protected abstract String ellipsis(String s, int max);

  protected record Labels(
      String ticket,
      String verify,
      String terminal,
      String outlet,
      String draw,
      String drawAt,
      String soldAt,
      String scan,
      String gameHdr,
      String selHdr,
      String stakeHdr,
      String winHdr,
      String totalStake,
      String totalWin) {}

  @Override
  public String formatText(TicketPrintView t, String verifyUrl) {
    // TODO(sales-refactor): restore structured receipt lines once receipt model classes are reinstated.
    var labels = labels();
    var ticketCode = t == null ? "-" : safe(t.ticketCode());
    var publicCode = t == null ? "-" : safe(t.publicCode());
    return title() + "\n"
        + labels.ticket() + ": " + sanitize(ticketCode) + "\n"
        + labels.verify() + ": " + sanitize(publicCode);
  }

  protected String safe(String value) {
    return value == null ? "-" : value;
  }
}
