package com.tchalanet.server.core.sales.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SellTicketLineRequest(
    @NotBlank String gameCode,
    @NotBlank String selection,
    @NotBlank String betType,
    Short betOption,
    @NotNull BigDecimal stakeAmount,
    @NotNull BigDecimal oddsSnapshot
) {}
