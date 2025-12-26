package com.tchalanet.server.core.draw.application.query.model;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.domain.model.DrawResult;

import java.util.UUID;

public record GetDrawResultQuery(TenantId tenantId, DrawId drawId) implements Query<DrawResult> {}
