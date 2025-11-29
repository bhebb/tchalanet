package com.tchalanet.server.core.pos.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Terminal(UUID id, UUID tenantId, UUID outletId, String state, Instant lastSeen) {}
