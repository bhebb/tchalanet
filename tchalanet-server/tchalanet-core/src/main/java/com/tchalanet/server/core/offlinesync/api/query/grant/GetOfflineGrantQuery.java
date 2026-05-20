package com.tchalanet.server.core.offlinesync.api.query.grant;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineGrantQuery(TenantId tenantId, OfflineGrantId grantId) {}
