package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeMode;
import com.tchalanet.server.common.types.id.UserId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

public record UserPreference(
    UserId userId,
    ThemeMode themeMode,
    Short density,
    Locale locale,
    ZoneId timeZone,
    Currency currency) {

  public static UserPreference forUser(UserId userId) {
    return new UserPreference(userId, null, null, null, null, null);
  }

  public UserPreference applyOverrides(
      ThemeMode themeMode, Short density, Locale locale, ZoneId timeZone, Currency currency) {
    return new UserPreference(
        userId,
        themeMode != null ? themeMode : this.themeMode,
        density != null ? density : this.density,
        locale != null ? locale : this.locale,
        timeZone != null ? timeZone : this.timeZone,
        currency != null ? currency : this.currency);
  }
}
