package com.tchalanet.server.core.session.application.ports.in;

import java.util.UUID;

public record ListCashierOpenSessionsQuery(UUID tenantId, UUID userId) {}

