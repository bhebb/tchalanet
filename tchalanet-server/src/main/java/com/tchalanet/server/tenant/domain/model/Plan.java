package com.tchalanet.server.tenant.domain.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record Plan(
    UUID id,
    String code,
    String name,
    BigDecimal priceAmount,
    String currency,
    Map<String, Object> features) {}
