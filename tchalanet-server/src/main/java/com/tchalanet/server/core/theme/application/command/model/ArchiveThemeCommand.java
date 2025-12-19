package com.tchalanet.server.core.theme.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record ArchiveThemeCommand(UUID tenantId, UUID themeId) implements Command<Void> {}

