package com.tchalanet.server.core.tenant.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanDTO(
    UUID id,
    String code,
    BigDecimal priceAmount,
    String currency,
    String frequency,
    boolean publicPlan,
    List<String> features) {}
