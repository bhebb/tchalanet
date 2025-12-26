package com.tchalanet.server.core.theme.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.UUID;

public record ListThemesQuery(
    TenantId tenantId, ThemeStatus status, boolean includeBase) implements Query<List<ThemeView>> {}

