package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotNull;

public record GetTicketQrPngQuery(@NotNull TicketId ticketId, int sizePx) implements Query<byte[]> {
  public GetTicketQrPngQuery {
    if (sizePx <= 0) sizePx = 280;
  }
}
