package com.tchalanet.server.catalog.theme.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.theme.domain.model.ThemeStatus;
import java.util.List;

public record ListThemesQuery(TenantId tenantId, ThemeStatus status, boolean includeBase)
    implements Query<List<ThemeView>> {}
