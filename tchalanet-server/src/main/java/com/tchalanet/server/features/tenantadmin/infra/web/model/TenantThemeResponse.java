package com.tchalanet.server.features.tenantadmin.infra.web.model;

import java.util.Map;

public record TenantThemeResponse(String presetId, Map<String, Object> overrides) {}
