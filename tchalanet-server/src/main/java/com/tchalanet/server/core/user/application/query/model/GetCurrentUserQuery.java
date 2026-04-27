package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

public record GetCurrentUserQuery(UserId userId) implements Query<CurrentUserDetails> {}
