package com.tchalanet.server.platform.tenant.api.model.view;

import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight tenant projection for list endpoints.
 * Details (address, theme, config, internal settings) belong in TenantConfigView.
 */
public record TenantSummaryView(
    UUID id,
    String code,
    String name,
    TenantType type,
    TenantStatus status,
    String currency,
    String timezone,
    BigDecimal defaultCommissionRate,
    Instant createdAt,
    Instant updatedAt
) {}
