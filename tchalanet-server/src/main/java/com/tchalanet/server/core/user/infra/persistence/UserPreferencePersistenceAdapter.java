package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPreferencePersistenceAdapter
    implements UserPreferenceReaderPort, UserPreferenceWriterPort {

  private final JpaUserPreferenceRepository jpaRepository;

  @Override
  public Optional<UserPreference> findByUserId(UserId id) {
    return jpaRepository.findById(id.uuid()).map(this::toDomain);
  }

  @Override
  public Optional<UserPreference> findActiveByUserId(UserId userId) {
    // For now, reuse findById
    return findByUserId(userId);
  }

  @Override
  public UserPreference save(UserPreference preference) {
    return null;
  }

  @Override
  public void softDelete(UserId userId, Instant when) {}

  @Override
  public void upsert(UserId userId, String themeMode, Short density, String locale) {}

  private UserPreference toDomain(UserPreferenceJpaEntity e) {
    UserPreference pref = new UserPreference();
    pref.setUserId(UserId.of(e.getUser().getId()));
    pref.setThemeMode(e.getThemeMode());
    pref.setDensity(e.getDensity());
    pref.setLocale(e.getLocale() != null ? java.util.Locale.forLanguageTag(e.getLocale()) : null);
    return pref;
  }

  private UserPreferenceJpaEntity toEntity(UserPreference pref) {
    UserPreferenceJpaEntity e = new UserPreferenceJpaEntity();
    AppUserJpaEntity user = new AppUserJpaEntity();
    user.setId(pref.getUserId().uuid());
    e.setUser(user);
    e.setThemeMode(pref.getThemeMode());
    e.setDensity(pref.getDensity());
    e.setLocale(pref.getLocale() != null ? pref.getLocale().toLanguageTag() : null);
    return e;
  }
}
