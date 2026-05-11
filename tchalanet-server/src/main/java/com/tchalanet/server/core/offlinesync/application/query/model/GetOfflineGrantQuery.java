package com.tchalanet.server.core.offlinesync.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;

public record GetOfflineGrantQuery(TenantId tenantId, OfflineSalesGrantId grantId)
    implements Query<OfflineSalesGrant> {}

