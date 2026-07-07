package com.tchalanet.server.platform.tenantgame.api.model.view;

import java.util.List;

public record TenantGameBetOptionConfigView(
    String gameCode,
    List<TenantBetTypeOptionConfigView> betTypes
) {}
