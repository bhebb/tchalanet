package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.stereotype.PersistenceAdapter;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUserId;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@PersistenceAdapter
@RequiredArgsConstructor
public class UserPreferencePersistenceAdapter implements UserPreferenceReaderPort, UserPreferenceWriterPort {

  private final JpaUserPreferenceRepository jpaRepository;

  @Override
  public Optional<UserPreference> findByUserId(UUID id) {
    return jpaRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<UserPreference> findActiveByUserId(UUID userId) {
    // Pour l'instant, on n'a pas de notion d'"active" distincte, on réutilise findById
    return findByUserId(userId);
  }

  @Override
  public UserPreferenceJpaEntity save(UserPreference preference) {

      return jpaRepository.save(toEntity(preference));
  }

  private UserPreference toDomain(UserPreferenceJpaEntity e) {
    UserPreference pref = new UserPreference();
    pref.setUserId(new AppUserId(e.getUser().getId()));
    pref.setThemeMode(e.getThemeMode());
    pref.setDensity(e.getDensity());
    pref.setLocale(e.getLocale() != null ? java.util.Locale.forLanguageTag(e.getLocale()) : null);
    return pref;
  }

  private UserPreferenceJpaEntity toEntity(UserPreference pref) {
    UserPreferenceJpaEntity e = new UserPreferenceJpaEntity();
    AppUserJpaEntity user = new AppUserJpaEntity();
    user.setId(pref.getUserId().value());
    e.setUser(user);
    e.setThemeMode(pref.getThemeMode());
    e.setDensity(pref.getDensity());
    e.setLocale(pref.getLocale() != null ? pref.getLocale().toLanguageTag() : null);
    return e;
  }
}

