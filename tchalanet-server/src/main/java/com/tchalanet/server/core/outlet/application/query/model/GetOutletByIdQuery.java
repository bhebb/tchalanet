package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;
import java.util.Optional;

public record GetOutletByIdQuery(UUID tenantId, UUID outletId) implements Query<Optional<com.tchalanet.server.core.outlet.domain.model.Outlet>> {}
