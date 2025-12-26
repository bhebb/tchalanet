package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApplyResultRow(
    TenantId tenantId,
    DrawId drawId,
    List<String> numbersMain,
    List<String> numbersExtra,
    Instant occurredAt,
    Map<String, Object> rawPayload) {}
