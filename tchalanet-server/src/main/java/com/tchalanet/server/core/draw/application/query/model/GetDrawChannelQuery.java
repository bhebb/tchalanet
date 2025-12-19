package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.util.UUID;

public record GetDrawChannelQuery(UUID tenantId, UUID id) implements Query<DrawChannel> {}
