package com.tchalanet.server.core.promotion.internal.domain.model;

import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public record PromotionRuleDefinition(
    PromotionRuleId id,
    TenantId tenantId,
    String code,
    String name,
    boolean active,
    String ruleType,
    String engineType,
    int schemaVersion,
    int ruleVersion,
    int priority,
    boolean stackable,
    String exclusiveGroup,
    Instant startsAt,
    Instant endsAt,
    ZoneId timezone,
    Map<String, Object> conditionJson,
    Map<String, Object> effectJson,
    boolean offlineAllowed
) {}
