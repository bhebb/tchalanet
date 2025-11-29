package com.tchalanet.server.core.accesscontrol.infra.web.dto;

import java.util.Set;

public record UpdateRolePermissionsRequest(Set<String> permissionCodes) {}
