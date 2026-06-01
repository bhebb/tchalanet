package com.tchalanet.server.platform.accesscontrol.api.model.result;

import java.util.List;

public record BootstrapAccessControlResult(
    String mode,
    int permissionsCreated,
    int rolesCreated,
    int roleMappingsCreated,
    int roleMappingsRemoved,
    List<String> validationErrors,
    boolean valid) {}
