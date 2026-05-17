package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import jakarta.validation.constraints.NotNull;

public record GetTicketDetailsQuery(
    @NotNull TicketId ticketId
) implements Query<TicketDetailsView> {}
