package com.tchalanet.server.platform.accesscontrol.internal.web.model;

import java.util.Set;

public record UpdateRolePermissionsRequest(Set<String> permissionCodes) {}
