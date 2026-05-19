package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;

/**
 * Looks up the offline sales policy for the given tenant. Result is the canonical input
 * for {@code OfflineGrantPolicy} and {@code OfflineSubmissionTechnicalPolicy}.
 */
public record GetOfflineLimitPolicyQuery(TenantId tenantId) implements Query<OfflineLimitPolicy> {}
