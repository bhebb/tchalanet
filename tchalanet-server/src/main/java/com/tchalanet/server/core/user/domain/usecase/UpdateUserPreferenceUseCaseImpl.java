package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.ThemeMode;
import com.tchalanet.server.core.user.domain.model.UserId;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import com.tchalanet.server.core.user.domain.ports.UserPreferenceRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Use case pour mettre à jour les préférences utilisateur. */
@Service
@RequiredArgsConstructor
public class UpdateUserPreferenceUseCaseImpl {

  private final UserPreferenceRepository repository;

  /** Met à jour la locale de l'utilisateur */
  public UserPreference updateLocale(UserId userId, Locale locale) {
    var pref =
        repository
            .findByUserId(userId)
            .orElseGet(
                () -> {
                  var newPref = new UserPreference();
                  newPref.setUserId(userId);
                  return newPref;
                });
    pref.setLocale(locale);
    return repository.save(pref);
  }

  /** Met à jour toutes les préférences de l'utilisateur */
  public UserPreference updatePreferences(
      UserId userId, ThemeMode themeMode, Short density, Locale locale) {
    var pref =
        repository
            .findByUserId(userId)
            .orElseGet(
                () -> {
                  var newPref = new UserPreference();
                  newPref.setUserId(userId);
                  return newPref;
                });

    if (themeMode != null) {
      pref.setThemeMode(themeMode);
    }
    if (density != null) {
      pref.setDensity(density);
    }
    if (locale != null) {
      pref.setLocale(locale);
    }

    return repository.save(pref);
  }

  /** Variante acceptant directement un UUID pour compatibilité */
  public UserPreference updatePreferences(
      UUID userId, ThemeMode themeMode, Short density, String localeStr) {
    Locale locale = localeStr != null ? Locale.forLanguageTag(localeStr) : null;
    return updatePreferences(new UserId(userId), themeMode, density, locale);
  }
}
