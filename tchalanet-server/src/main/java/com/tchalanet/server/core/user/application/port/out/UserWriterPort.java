package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public interface UserWriterPort {
    AppUser save(AppUser user);

    void softDelete(UUID userId, Instant when);

    void updateStatus(UUID uuid, UserStatus userStatus);
}

