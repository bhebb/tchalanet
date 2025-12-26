package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.user.domain.model.AppUser;
import org.springframework.data.domain.Page;

public record PagedListTenantUsersQuery(TenantId tenantId, int page, int size)
    implements Query<Page<AppUser>> {}
