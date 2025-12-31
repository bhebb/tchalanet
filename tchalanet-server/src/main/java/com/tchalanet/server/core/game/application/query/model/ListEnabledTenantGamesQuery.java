package com.tchalanet.server.core.game.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.List;

public record ListEnabledTenantGamesQuery() implements Query<List<TenantGame>> {}
