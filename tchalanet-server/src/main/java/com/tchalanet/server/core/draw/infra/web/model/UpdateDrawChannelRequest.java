package com.tchalanet.server.core.draw.infra.web.model;

import java.util.UUID;

public record UpdateDrawChannelRequest(
    UUID tenantId, UUID id, String code, String name, boolean active) {}
