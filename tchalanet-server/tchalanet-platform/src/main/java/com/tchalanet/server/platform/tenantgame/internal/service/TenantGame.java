package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Tenant game domain model — tenant-owned fields only.
 * Catalog fields (name, category, combination, minDigits, maxDigits) are NOT stored here.
 * Fetch them from {@code GameCatalog} when needed for display/validation.
 */
public record TenantGame(
    TenantGameId tenantGameId,
    TenantId tenantId,
    GameId gameId,
    String gameCode,
    boolean enabled,
    boolean visibleInPos,
    String displayName,
    int displayOrder,
    BigDecimal minStake,
    BigDecimal maxStake,
    boolean availabilityEnabled,
    String availabilityDays,
    LocalTime startLocalTime,
    LocalTime endLocalTime
) {}
