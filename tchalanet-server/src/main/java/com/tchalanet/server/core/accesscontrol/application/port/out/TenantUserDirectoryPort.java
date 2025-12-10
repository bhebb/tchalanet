package com.tchalanet.server.core.accesscontrol.application.port.out;

import java.util.List;

public interface TenantUserDirectoryPort {
    List<String> getUserRolesInTenant(String userId, String tenantId);
    boolean isUserActiveInTenant(String userId, String tenantId);
}
