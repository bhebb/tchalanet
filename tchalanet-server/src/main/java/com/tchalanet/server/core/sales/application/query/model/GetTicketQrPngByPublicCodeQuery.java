package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import jakarta.validation.constraints.NotBlank;

public record GetTicketQrPngByPublicCodeQuery(@NotBlank String publicCode, int sizePx)
    implements Query<byte[]> {

  public GetTicketQrPngByPublicCodeQuery {
    if (sizePx <= 0) sizePx = 280;
  }
}
