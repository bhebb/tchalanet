package com.tchalanet.server.accesscontrol.infra.web.dto;

import java.util.Set;

public record UpdateRolePermissionsRequest(Set<String> permissionCodes) {}
