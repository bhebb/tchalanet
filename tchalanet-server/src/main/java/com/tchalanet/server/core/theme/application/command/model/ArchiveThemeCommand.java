package com.tchalanet.server.core.theme.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record ArchiveThemeCommand(TenantId tenantId, UUID themeId) implements Command<Void> {}
