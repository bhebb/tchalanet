package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserPreference;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceReaderPort {
    Optional<UserPreference> findByUserId(UUID id);
    Optional<UserPreference> findActiveByUserId(UUID userId);

}

