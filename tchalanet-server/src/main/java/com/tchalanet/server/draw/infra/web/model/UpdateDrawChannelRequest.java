package com.tchalanet.server.draw.infra.web.model;

import java.util.UUID;

public record UpdateDrawChannelRequest(
    UUID tenantId, UUID id, String code, String name, boolean active) {}
