package com.tchalanet.server.core.theme.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

public record GetThemeByIdQuery(TenantId tenantId, UUID themeId)
    implements Query<ThemeView> {}

