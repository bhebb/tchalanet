package com.tchalanet.server.core.sales.api.command.sell;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;

import java.util.List;

public record SellTicketCommand(
    DrawId drawId,
    DrawChannelId drawChannelId,
    CurrencyCode currency,
    List<SellTicketLineInput> lines,
    SaleCommunicationOptions communicationOptions,
    List<PromotionChoiceInput> promotionChoices
) implements Command<SellTicketResult> {
}
