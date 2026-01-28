package com.tchalanet.server.features.tenantadmin.infra.web.model;

import java.util.Map;

public record TenantThemeRequest(String presetId, Map<String, Object> overrides) {}
