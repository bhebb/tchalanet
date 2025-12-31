package com.tchalanet.server.core.game.infra.web.model;

import java.util.List;

public record EnsureTenantGamesResponse(
    List<String> requested, List<String> created, List<String> already) {}
