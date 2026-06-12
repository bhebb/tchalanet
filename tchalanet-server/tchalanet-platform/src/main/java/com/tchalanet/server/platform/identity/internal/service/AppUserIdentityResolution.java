package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.platform.identity.api.model.UserStatus;
import java.util.UUID;

public record AppUserIdentityResolution(UUID appUserId, UserStatus status) {}

