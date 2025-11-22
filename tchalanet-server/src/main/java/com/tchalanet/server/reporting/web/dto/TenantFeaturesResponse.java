package com.tchalanet.server.reporting.web.dto;

import java.util.List;

public record TenantFeaturesResponse(
    String tenantId, List<String> enabledFeatures, java.util.Map<String, Object> featureConfig) {}
