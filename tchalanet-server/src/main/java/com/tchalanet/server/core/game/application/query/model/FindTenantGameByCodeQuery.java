package com.tchalanet.server.core.game.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.Optional;

public record FindTenantGameByCodeQuery(String code) implements Query<Optional<TenantGame>> {}
