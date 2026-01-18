package com.tchalanet.server.catalog.game.application.command.model;

import com.tchalanet.server.catalog.game.application.service.TenantGameEnsureService;
import com.tchalanet.server.common.bus.Command;
import java.util.List;

public record EnsureTenantGamesCommand(List<String> codes)
    implements Command<
        TenantGameEnsureService.EnsureResult> {}
