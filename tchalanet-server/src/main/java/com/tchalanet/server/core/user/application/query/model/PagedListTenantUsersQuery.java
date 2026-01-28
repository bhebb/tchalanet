package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public record PagedListTenantUsersQuery(TenantId tenantId, TchPageRequest pageRequest)
    implements Query<TchPage<UserRow>> {}
