package com.tchalanet.server.core.theme.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record PublishThemeCommand(UUID tenantId, UUID themeId, int themeVersion)
    implements Command<Void> {}





