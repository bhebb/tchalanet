package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.domain.model.DrawResult;

import java.util.UUID;

public record GetDrawResultQuery(UUID tenantId, UUID drawId) implements Query<DrawResult> {}
