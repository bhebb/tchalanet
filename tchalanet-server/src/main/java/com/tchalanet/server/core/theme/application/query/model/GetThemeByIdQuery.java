package com.tchalanet.server.core.theme.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

public record GetThemeByIdQuery(UUID tenantId, UUID themeId)
    implements Query<ThemeView> {}

