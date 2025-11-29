package com.tchalanet.server.core.pos.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CloseSessionRequest(@NotNull UUID userId, BigDecimal closingAmount) {}
