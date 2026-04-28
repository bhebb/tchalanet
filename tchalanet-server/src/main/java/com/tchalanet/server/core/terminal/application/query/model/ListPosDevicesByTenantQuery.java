package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.util.List;

public record ListPosDevicesByTenantQuery(TenantId tenantId) implements Query<List<Terminal>> {}
