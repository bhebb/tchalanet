package com.tchalanet.server.platform.tenantgame.api.model.view;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.util.List;

public record TenantBetTypeOptionConfigView(
    BetType betType,
    SelectionPolicy selectionPolicy,
    Short defaultOption,
    List<TenantBetOptionView> options
) {}
