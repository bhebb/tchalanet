package com.tchalanet.server.core.pagemodel.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public record ResolveEffectivePageModelQuery(Optional<TenantId> tenantId, String logicalId) implements Query<com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc> {}
