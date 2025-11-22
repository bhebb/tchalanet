package com.tchalanet.server.common.web.dto;

import java.util.List;

public record TenantFeaturesDto(String tenant, String role, List<String> features) {}
