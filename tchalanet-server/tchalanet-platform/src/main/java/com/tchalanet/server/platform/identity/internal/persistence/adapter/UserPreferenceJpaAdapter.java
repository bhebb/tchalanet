package com.tchalanet.server.platform.identity.internal.persistence.adapter;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.UserPreferenceJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.mapper.IdentityPersistenceMapper;
import com.tchalanet.server.platform.identity.internal.persistence.repository.UserPreferenceJpaRepository;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPreferenceJpaAdapter {

  private final UserPreferenceJpaRepository repository;

  public Optional<UserPreference> findByUserId(UserId userId) {
    return repository.findById(userId.value()).map(IdentityPersistenceMapper::toPreference);
  }

  public UserPreference upsert(UserPreference preference) {
    var entity =
        repository
            .findById(preference.userId().value())
            .orElseGet(
                () -> {
                  var created = new UserPreferenceJpaEntity();
                  var user = new AppUserJpaEntity();
                  user.setId(preference.userId().value());
                  created.setId(preference.userId().value());
                  created.setUser(user);
                  return created;
                });

    entity.setThemeMode(preference.themeMode());
    entity.setDensity(preference.density());
    entity.setLocale(preference.locale());
    entity.setTimeZone(preference.timeZone());
    entity.setCurrency(preference.currency());
    return IdentityPersistenceMapper.toPreference(repository.save(entity));
  }
}
