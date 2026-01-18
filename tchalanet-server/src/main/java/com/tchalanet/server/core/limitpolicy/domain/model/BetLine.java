package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BetType;
import java.math.BigDecimal;

public record BetLine(
    BetType betType,
    String selection,
    BigDecimal stake,
    Short betOption
) {}

