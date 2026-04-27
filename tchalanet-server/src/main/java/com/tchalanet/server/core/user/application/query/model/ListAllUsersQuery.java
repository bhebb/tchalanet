package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.List;

public record ListAllUsersQuery() implements Query<List<AppUser>> {}
