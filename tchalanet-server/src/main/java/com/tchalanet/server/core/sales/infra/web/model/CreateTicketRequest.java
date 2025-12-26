package com.tchalanet.server.core.sales.infra.web.model;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateTicketRequest(
    @NotNull UUID terminalId,
    @NotNull UUID drawId, // Or drawChannelCode + scheduledAt for resolution
    @NotNull @Valid List<LineRequest> lines) {
  public record LineRequest(
      @NotBlank String gameCode, @NotBlank String selection, @NotNull @Min(0) BigDecimal stake) {}
}
