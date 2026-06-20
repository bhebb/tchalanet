package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.catalog.theme.api.ThemeMode;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import java.util.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserProfileService {

  private final AppUserJpaAdapter users;
  private final UserPreferenceJpaAdapter preferences;
  private final TchContextResolver contextResolver;

  public CurrentUserView getCurrentUser(UserId userId) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    var pref = preferences.findByUserId(userId).orElse(null);
    var ctx = contextResolver.currentOrNull();

    var requestLocale = ctx != null && ctx.locale() != null ? ctx.locale().toLanguageTag() : null;
    var tenantTimeZone = ctx != null && ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : null;
    var tenantCurrency = ctx != null && ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : null;
    var tenantId = ctx == null ? null : ctx.tenantIdSafe();
    var tenantCode = ctx == null ? null : ctx.effectiveTenantCode();

    var prefLocale = pref != null && pref.locale() != null ? pref.locale().toLanguageTag() : null;
    var prefTimeZone = pref != null && pref.timeZone() != null ? pref.timeZone().getId() : null;
    var prefCurrency = pref != null && pref.currency() != null ? pref.currency().getCurrencyCode() : null;

    return new CurrentUserView(
        user.id(),
        user.username(),
        user.email(),
        user.firstName(),
        user.lastName(),
        user.displayName(),
        tenantId,
        tenantCode,
        tenantTimeZone,
        tenantCurrency,
        pref == null || pref.themeMode() == null ? ThemeMode.SYSTEM : pref.themeMode(),
        pref == null ? null : pref.density(),
        firstNonBlank(prefLocale, requestLocale, "fr"),
        firstNonBlank(prefTimeZone, tenantTimeZone, "America/Port-au-Prince"),
        firstNonBlank(prefCurrency, tenantCurrency, "USD"),
        user.mustChangePassword(),
        user.mustCompleteProfile(),
        user.firstLoginCompletedAt() == null ? null : user.firstLoginCompletedAt().toString(),
        user.temporaryCredentialIssuedAt() == null ? null : user.temporaryCredentialIssuedAt().toString());
  }

  public UserProfileView getUserProfile(UserId userId) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    var pref = preferences.findByUserId(userId).orElse(null);
    return new UserProfileView(
        user.id(),
        user.username(),
        user.email(),
        user.phone(),
        user.status(),
        user.firstName(),
        user.lastName(),
        user.displayName(),
        pref == null ? null : pref.themeMode(),
        pref == null ? null : pref.density(),
        pref == null || pref.locale() == null ? null : pref.locale().toLanguageTag(),
        pref == null || pref.timeZone() == null ? null : pref.timeZone().getId(),
        pref == null || pref.currency() == null ? null : pref.currency().getCurrencyCode());
  }

  @Transactional
  public void updateProfile(UpdateUserProfileRequest request) {
    var existing =
        users
            .findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));
    var firstName = request.firstName().orElse(existing.firstName());
    var lastName = request.lastName().orElse(existing.lastName());
    var displayName = (firstName + " " + lastName).trim();
    var saved =
        users.save(
            existing.syncProfile(
                existing.username(),
                request.email().orElse(existing.email()),
                request.phone().orElse(existing.phone()),
                firstName,
                lastName,
                displayName,
                existing.avatarUrl()));

    var currentPref = preferences.findByUserId(saved.id()).orElseGet(() -> UserPreference.forUser(saved.id()));
    preferences.upsert(
        currentPref.applyOverrides(null, null, request.locale().orElse(null), null, null));
  }

  public void updatePreferences(
      UserId userId,
      ThemeMode themeMode,
      Short density,
      java.util.Locale locale,
      java.time.ZoneId timeZone,
      Currency currency) {
    var pref = preferences.findByUserId(userId).orElseGet(() -> UserPreference.forUser(userId));
    preferences.upsert(pref.applyOverrides(themeMode, density, locale, timeZone, currency));
  }

  private static String firstNonBlank(String... values) {
    for (var value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
