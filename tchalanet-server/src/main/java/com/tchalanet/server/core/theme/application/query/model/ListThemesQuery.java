package com.tchalanet.server.core.theme.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.UUID;

public record ListThemesQuery(
    UUID tenantId, ThemeStatus status, boolean includeBase) implements Query<List<ThemeView>> {}

