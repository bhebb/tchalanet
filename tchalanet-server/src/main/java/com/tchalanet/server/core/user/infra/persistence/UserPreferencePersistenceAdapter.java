package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
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
    return jpaRepository.findById(id.value()).map(this::toDomain);
  }

  // Backward-compatible helper (not part of the interface)
  public Optional<UserPreference> findActiveByUserId(UserId userId) {
    return findByUserId(userId);
  }

  // Basic save helper (not part of the interface)
  public UserPreference save(UserPreference preference) {
    UserPreferenceJpaEntity e = toEntity(preference);
    var saved = jpaRepository.save(e);
    return toDomain(saved);
  }

  // Soft-delete helper (not part of the interface)
  public void softDelete(UserId userId, Instant when) {
    jpaRepository.findById(userId.value()).ifPresent(e -> {
      e.setDeletedAt(when);
      jpaRepository.save(e);
    });
  }

  // Backward-compatible upsert helper that accepts locale/timeZone/currency as strings
  public void upsert(UserId userId, String themeMode, Short density, String locale, String timeZone, String currency) {
    Locale loc = locale == null ? null : Locale.forLanguageTag(locale);
    ZoneId zid = timeZone == null ? null : ZoneId.of(timeZone);
    Currency cur = currency == null ? null : Currency.getInstance(currency);
    upsert(userId, themeMode, density, loc, zid, cur);
  }

  // New upsert with typed Locale/ZoneId/Currency (not part of the port interface)
  public void upsert(UserId userId, String themeMode, Short density, Locale locale, ZoneId timeZone, Currency currency) {
    var entityOpt = jpaRepository.findById(userId.value());
    UserPreferenceJpaEntity e = entityOpt.orElseGet(() -> {
      UserPreferenceJpaEntity ne = new UserPreferenceJpaEntity();
      AppUserJpaEntity user = new AppUserJpaEntity();
      user.setId(userId.value());
      ne.setUser(user);
      return ne;
    });

    e.setThemeMode(themeMode == null ? null : com.tchalanet.server.common.types.enums.ThemeMode.valueOf(themeMode));
    e.setDensity(density);
    e.setLocale(locale);
    e.setTimeZone(timeZone);
    e.setCurrency(currency);

    jpaRepository.save(e);
  }

  // Implement writer port convenience: upsert(UserPreference pref)
  @Override
  public UserPreference upsert(UserPreference pref) {
    if (pref == null || pref.userId() == null) throw new IllegalArgumentException("pref.userId is required");
    // delegate to typed upsert
    Locale locale = pref.locale();
    ZoneId zid = pref.timeZone();
    Currency cur = pref.currency();

    upsert(pref.userId(), pref.themeMode() == null ? null : pref.themeMode().name(), pref.density(), locale, zid, cur);
    // return the saved domain object (read back)
    return findByUserId(pref.userId()).orElse(pref);
  }

  private UserPreference toDomain(UserPreferenceJpaEntity e) {
    // domain record uses typed Locale/ZoneId/Currency
    Locale locale = e.getLocale();
    ZoneId tz = e.getTimeZone();
    Currency cur = e.getCurrency();

    return new UserPreference(
        UserId.of(e.getUser().getId()),
        e.getThemeMode(),
        e.getDensity(),
        locale,
        tz,
        cur);
  }

  private UserPreferenceJpaEntity toEntity(UserPreference pref) {
    UserPreferenceJpaEntity e = new UserPreferenceJpaEntity();
    AppUserJpaEntity user = new AppUserJpaEntity();
    user.setId(pref.userId().value());
    e.setUser(user);
    e.setThemeMode(pref.themeMode());
    e.setDensity(pref.density());
    // domain fields are typed already (Locale/ZoneId/Currency)
    e.setLocale(pref.locale());
    e.setTimeZone(pref.timeZone());
    e.setCurrency(pref.currency());
    return e;
  }
}
