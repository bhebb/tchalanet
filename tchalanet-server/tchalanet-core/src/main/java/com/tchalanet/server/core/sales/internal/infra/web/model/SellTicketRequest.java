package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SellTicketRequest(
    @NotNull DrawId drawId,
    @NotNull DrawChannelId drawChannelId,
    @NotNull CurrencyCode currency,
    @NotEmpty @Valid List<SellTicketLineRequest> lines,
    SaleCommunicationOptions serviceOptions
) {
}
