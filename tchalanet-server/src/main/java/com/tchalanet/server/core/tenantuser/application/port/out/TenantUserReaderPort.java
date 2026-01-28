package com.tchalanet.server.core.tenantuser.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserDetails;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserSearchCriteria;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;

import java.util.Optional;

public interface TenantUserReaderPort {
    TchPage<TenantUserRow> pagedListByTenant(TenantId tenantId, TchPageRequest pageReq);

    Optional<TenantUserMembership> findByTenantIdAndUserId(TenantId tenantId, UserId userId);
}
