package com.tchalanet.server.core.sales.infra.web.model;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.BetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SellTicketRequest(
    @NotNull UUID tenantId,
    @NotNull UUID terminalId,
    @NotNull UUID cashierId,
    @NotNull UUID drawId,
    @NotBlank String currency,
    @NotNull @Valid List<LineRequest> lines) {
  public record LineRequest(
      @NotBlank String gameCode, @NotBlank String selection, @NotNull @Min(0) BigDecimal stake, @NotNull BetType betType) {}
}
