package com.tchalanet.server.core.sales.infra.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SellTicketRequest(
    @NotNull BigDecimal feeAmount,
    @NotEmpty List<@Valid SellTicketLineRequest> lines
) {}
