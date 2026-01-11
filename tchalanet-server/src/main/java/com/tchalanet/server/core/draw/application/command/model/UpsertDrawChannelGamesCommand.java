package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

public record UpsertDrawChannelGamesCommand(
    TenantId tenantId, DrawChannelId channelId, List<String> gameCodes, boolean enabled)
    implements Command<Void> {}
