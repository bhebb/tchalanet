package com.tchalanet.server.features.tenantadmin.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public record PagedListTenantUsersQuery(TenantId tenantId, TchPageRequest pageRequest) implements Query<com.tchalanet.server.common.web.paging.TchPage<com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserRow>> {}
