package com.tchalanet.server.core.sales.application.print;

import com.tchalanet.server.common.print.receipt.ReceiptModel;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;

public interface TicketReceiptFormatter {
  ReceiptModel formatModel(
      TicketPrintView t, String verifyUrl);

  // utile pour debug/logs si tu veux
  default String formatText(TicketPrintView t, String verifyUrl) {
    var m = formatModel(t, verifyUrl);
    var sb = new StringBuilder();
    sb.append(m.title()).append('\n');
    for (var line : m.lines()) {
      for (var sp : line.spans()) sb.append(sp.text());
      sb.append('\n');
    }
    return sb.toString();
  }
}
