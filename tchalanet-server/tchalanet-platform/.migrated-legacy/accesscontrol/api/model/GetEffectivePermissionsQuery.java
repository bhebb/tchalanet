package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetEffectivePermissionsRequest(UserId userId, TenantId tenantId)
	implements Query<EffectivePermissionsView> {}

