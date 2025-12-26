package com.tchalanet.server.core.accesscontrol.infra.web.model;

import java.util.Set;

public record UpdateRolePermissionsRequest(Set<String> permissionCodes) {}
