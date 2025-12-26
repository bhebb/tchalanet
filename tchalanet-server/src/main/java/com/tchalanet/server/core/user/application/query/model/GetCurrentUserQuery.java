package com.tchalanet.server.core.user.application.query.model;
import com.tchalanet.server.common.types.id.UserId;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

public record GetCurrentUserQuery(UserId userId) implements Query<UserProfileQuery> {}

