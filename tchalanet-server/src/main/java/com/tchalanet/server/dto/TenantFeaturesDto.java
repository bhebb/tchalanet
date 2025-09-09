package com.tchalanet.server.dto;

import java.util.List;

public record TenantFeaturesDto(String tenant, String role, List<String> features) {}
