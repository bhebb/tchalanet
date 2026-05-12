package com.tchalanet.server.platform.identity.internal.web.model;

import java.util.Map;

public record UserContextResponse(
    String sub,
    java.util.List<String> roles,
    String activeEnterpriseId,
    boolean isSuperAdmin,
    Map<String, Object> metadata) {}
