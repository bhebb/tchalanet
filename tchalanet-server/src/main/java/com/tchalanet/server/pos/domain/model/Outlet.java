package com.tchalanet.server.pos.domain.model;

import java.util.UUID;

public record Outlet(UUID id, UUID tenantId, String name, String zone) {}
