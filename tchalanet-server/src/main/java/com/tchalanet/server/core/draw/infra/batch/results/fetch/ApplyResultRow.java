package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ApplyResultRow(UUID tenantId, UUID drawId, List<String> numbersMain, List<String> numbersExtra, Instant occurredAt, Map<String, Object> rawPayload) {}
