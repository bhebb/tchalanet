package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Resolves the effective limits at TENANT and DRAW_CHANNEL scopes for a draw.
 *
 * <p>Does not include SELLER_TERMINAL resolution — this query is used by the admin BFF overview
 * which is not contextualized to a specific terminal.
 */
public record GetEffectiveLimitsForDrawQuery(TenantId tenantId, DrawChannelId drawChannelId)
    implements Query<EffectiveLimitsForDrawView> {}
