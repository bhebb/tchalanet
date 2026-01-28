package com.tchalanet.server.features.tenantadmin.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserDetails;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserRow;

public interface TenantAdminReaderPort {
  TchPage<TenantUserRow> pagedListByTenant(TenantId tenantId, TchPageRequest pageReq);

  TenantUserDetails getDetails(TenantId tenantId, UserId userId);
}
