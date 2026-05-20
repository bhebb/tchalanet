package com.tchalanet.server.core.sales.api.query.preview;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import java.util.List;

public record PreviewTicketSaleQuery(
    DrawId drawId,
    DrawChannelId drawChannelId,
    CurrencyCode currency,
    List<SellTicketLineInput> lines
) implements Query<TicketSalePreviewResult> {
    public PreviewTicketSaleQuery {
        lines = List.copyOf(lines);
    }
}
