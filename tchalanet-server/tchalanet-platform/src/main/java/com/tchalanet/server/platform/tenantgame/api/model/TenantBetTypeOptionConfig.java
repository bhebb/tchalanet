package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.catalog.game.api.model.BetType;
import java.util.List;

public record TenantBetTypeOptionConfig(
    BetType betType,
    SelectionPolicy selectionPolicy,
    Short defaultOption,
    List<TenantBetOptionConfig> options
) {}
