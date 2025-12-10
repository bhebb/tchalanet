package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

public record GetUserDetailsQuery(UUID keycloakUserId) implements Query<UserProfileQuery> {}
