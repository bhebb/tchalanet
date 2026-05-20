package com.tchalanet.server.platform.tenantconfig.api.model.view;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record TenantInternalRules(List<JsonNode> values) {}

