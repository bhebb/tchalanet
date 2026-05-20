package com.tchalanet.server.platform.identity.internal.web.model;

import com.tchalanet.server.common.types.id.UserId;

public record UserItemResponse(UserId id, String username, String email, String displayName, String status) {}


