package com.tchalanet.server.core.sales.api.command.preparation;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Prepares a sale server-side: runs the full sell pipeline (lines, charges,
 * money, promotion evaluation + auto-generated free line, limits) and persists
 * a {@code SalePreparation} (DRAFT, TTL 10 min) so confirm can persist exactly
 * the previewed lines. This is a Command, not a Query — it writes state
 * (maryaj-gratis-auto-selection-v1 design §1).
 */
public record PrepareSaleCommand(
    @NotNull DrawId drawId,
    @NotNull DrawChannelId drawChannelId,
    @NotNull CurrencyCode currency,
    @NotEmpty List<SellTicketLineInput> lines,
    SaleCommunicationOptions communicationOptions
) implements Command<SalePreparationView> {}
