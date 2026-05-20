package com.tchalanet.server.catalog.drawchannel.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.GameId;

/**
 * Search criteria for draw channel search. When used in tenant-scoped catalog, tenantId must
 * be non-null. For platform search tenantId may be null to indicate cross-tenant query.
 */
public record DrawChannelSearchCriteria(
    TenantId tenantId,
    String code,
    String nameContains,
    ResultSlotId resultSlotId,
    String externalProvider,
    Boolean active
) {}
