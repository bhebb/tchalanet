package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;

public interface TicketReceiptFormatter {
  // TODO(sales-refactor): restore structured receipt model when receipt package is stabilized.
  String formatText(TicketPrintView t, String verifyUrl);
}
