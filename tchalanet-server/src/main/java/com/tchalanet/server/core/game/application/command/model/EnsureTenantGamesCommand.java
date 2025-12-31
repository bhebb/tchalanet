package com.tchalanet.server.core.game.application.command;

import com.tchalanet.server.common.bus.Command;
import java.util.List;

public record EnsureTenantGamesCommand(List<String> codes) implements Command<com.tchalanet.server.core.game.application.service.TenantGameEnsureService.EnsureResult> {}

