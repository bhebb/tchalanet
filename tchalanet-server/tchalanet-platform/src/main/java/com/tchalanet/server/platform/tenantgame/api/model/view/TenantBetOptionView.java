package com.tchalanet.server.platform.tenantgame.api.model.view;

public record TenantBetOptionView(
    Short code,
    String label,
    String description,
    boolean enabled,
    boolean visibleInPos,
    int displayOrder) {}
