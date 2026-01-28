package com.tchalanet.server.core.tenantuser.application.query.model;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import java.util.Optional;

public record TenantUserSearchCriteria(
    Optional<String> text, // search across username/displayName/email
    Optional<RoleId> roleId,
    Optional<TenantUserStatus> status,
    Optional<AutonomyLevel> autonomyLevel) {}
