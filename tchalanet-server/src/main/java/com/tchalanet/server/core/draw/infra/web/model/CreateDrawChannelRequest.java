package com.tchalanet.server.core.draw.infra.web.model;

import java.util.UUID;

public record CreateDrawChannelRequest(UUID tenantId, String code, String name, boolean active) {}
