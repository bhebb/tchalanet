package com.tchalanet.server.catalog.theme.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record PublishThemeCommand(TenantId tenantId, UUID themeId, int themeVersion)
    implements Command<Void> {}
