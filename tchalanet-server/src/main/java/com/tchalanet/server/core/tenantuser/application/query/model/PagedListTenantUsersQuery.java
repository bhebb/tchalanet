package com.tchalanet.server.core.tenantuser.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public record PagedListTenantUsersQuery(TenantId tenantId, TchPageRequest pageRequest) implements Query<org.springframework.data.domain.Page<TenantUserRow>> {}
