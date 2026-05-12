package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.types.id.UserId;

public record GetCurrentUserRequest(UserId userId) {}
