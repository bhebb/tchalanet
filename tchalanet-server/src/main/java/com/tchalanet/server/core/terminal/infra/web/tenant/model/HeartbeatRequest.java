package com.tchalanet.server.core.terminal.infra.web.tenant.model;

import java.time.Instant;

public record HeartbeatRequest(Instant occurredAt) {
}
