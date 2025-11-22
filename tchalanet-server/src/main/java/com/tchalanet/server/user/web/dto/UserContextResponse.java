package com.tchalanet.server.user.web.dto;

import java.util.Map;

public record UserContextResponse(
    String sub,
    java.util.List<String> roles,
    String activeEnterpriseId,
    boolean isSuperAdmin,
    Map<String, Object> metadata) {}
