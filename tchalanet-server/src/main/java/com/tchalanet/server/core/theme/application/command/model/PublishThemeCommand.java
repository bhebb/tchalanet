package com.tchalanet.server.core.theme.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record PublishThemeCommand(TenantId tenantId, UUID themeId, int themeVersion)
    implements Command<Void> {}





