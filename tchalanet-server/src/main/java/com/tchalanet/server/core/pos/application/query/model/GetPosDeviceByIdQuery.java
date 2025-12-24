package com.tchalanet.server.core.pos.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.Optional;
import java.util.UUID;

public record GetPosDeviceByIdQuery(UUID tenantId, UUID deviceId) implements Query<Optional<Terminal>> {}
