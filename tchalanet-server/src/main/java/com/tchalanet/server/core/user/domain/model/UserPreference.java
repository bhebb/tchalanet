package com.tchalanet.server.core.user.domain.model;

import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.UserId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/** Préférences utilisateur côté domaine (immutable record with typed fields). */
public record UserPreference(
    UserId userId,
    ThemeMode themeMode,
    Short density,
    Locale locale,
    ZoneId timeZone,
    Currency currency) {

  public static UserPreference forUser(UserId userId) {
    Objects.requireNonNull(userId, "userId is required");
    return new UserPreference(userId, null, null, null, null, null);
  }

  /**
   * Return a new UserPreference instance with overrides applied (null values are ignored).
   */
  public UserPreference applyOverrides(
      ThemeMode themeMode, Short density, Locale locale, ZoneId timeZone, Currency currency) {
    return new UserPreference(
        this.userId,
        themeMode != null ? themeMode : this.themeMode,
        density != null ? density : this.density,
        locale != null ? locale : this.locale,
        timeZone != null ? timeZone : this.timeZone,
        currency != null ? currency : this.currency);
  }

  // Backwards-compatible getters (some code calls getX())
  public UserId getUserId() {
    return userId();
  }

  public ThemeMode getThemeMode() {
    return themeMode();
  }

  public Short getDensity() {
    return density();
  }

  public Locale getLocale() {
    return locale();
  }

  public ZoneId getTimeZone() {
    return timeZone();
  }

  public Currency getCurrency() {
    return currency();
  }
}
