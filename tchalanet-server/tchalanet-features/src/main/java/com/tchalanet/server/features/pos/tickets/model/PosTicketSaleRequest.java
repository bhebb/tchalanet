package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public record PosTicketSaleRequest(
    SellerTerminalId sellerTerminalId,
    String terminalId,
    @NotNull DrawId drawId,
    @NotNull DrawChannelId drawChannelId,
    @NotNull String currency,
    @NotEmpty @Valid List<PosTicketSaleLineRequest> lines,
    SaleCommunicationOptions serviceOptions) {

  public List<SellTicketLineInput> toLines() {
    var result = new ArrayList<SellTicketLineInput>(lines.size());
    for (var index = 0; index < lines.size(); index++) {
      result.add(lines.get(index).toLine(index + 1));
    }
    return List.copyOf(result);
  }

  public CurrencyCode currencyCode() {
    return CurrencyCode.of(currency);
  }
}
