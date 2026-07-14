package com.tchalanet.server.platform.tenantgame.api.model;

public record TenantBetOptionConfig(
    Short code, boolean enabled, boolean visibleInPos, int displayOrder) {}
