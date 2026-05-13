package com.tchalanet.server.core.sales.internal.application.print;

import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintView;

public interface TicketReceiptFormatter {
  // TODO(sales-refactor): restore structured receipt model when receipt package is stabilized.
  String formatText(TicketPrintView t, String verifyUrl);

  default String formatModel(TicketPrintView t, String verifyUrl) {
    return formatText(t, verifyUrl);
  }
}
