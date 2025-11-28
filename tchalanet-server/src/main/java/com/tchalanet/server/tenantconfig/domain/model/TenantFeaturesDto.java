package com.tchalanet.server.tenantconfig.domain.model;

import java.util.List;

public record TenantFeaturesDto(String tenant, String role, List<String> features) {}
