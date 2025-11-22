package com.tchalanet.server.user.domain.ports;

import com.tchalanet.server.user.domain.model.UserId;
import com.tchalanet.server.user.domain.model.UserPreference;
import java.util.Optional;
import java.util.UUID;

/** Port de persistance des préférences utilisateur (côté domaine). */
public interface UserPreferenceRepository {

  Optional<UserPreference> findByUserId(UserId userId);

  UserPreference save(UserPreference preference);

  Optional<UserPreference> findById(UUID userId);
}
