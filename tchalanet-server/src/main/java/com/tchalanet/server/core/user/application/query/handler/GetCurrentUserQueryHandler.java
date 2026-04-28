package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.query.model.CurrentUserDetails;
import com.tchalanet.server.core.user.application.query.model.EffectiveUiContext;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import com.tchalanet.server.core.user.application.query.model.TenantContext;
import com.tchalanet.server.core.user.application.query.model.UserPreferenceDetails;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentUserQueryHandler implements QueryHandler<GetCurrentUserQuery, CurrentUserDetails> {

  private final UserReaderPort userReaderPort;
  private final UserPreferenceReaderPort prefReaderPort;
  private final TchContextResolver ctxResolver;

  @Override
  public CurrentUserDetails handle(GetCurrentUserQuery query) {
    var user = userReaderPort.findById(query.userId()).orElseThrow(() -> new IllegalArgumentException("User not found: " + query.userId()));

    var ctx = ctxResolver.currentOrNull();

    String tenantTz = ctx != null && ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : null;
    String tenantCurrency = ctx != null && ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : null;
    String tenantCode = ctx != null ? ctx.effectiveTenantCode() : null;
    TenantId tenantId = ctx != null ? ctx.tenantIdSafe() : null;

    String requestLocale = ctx != null && ctx.locale() != null ? ctx.locale().toLanguageTag() : null;

    var pref = prefReaderPort.findByUserId(query.userId()).orElse(null);

    var prefDetails = new UserPreferenceDetails(
        pref != null ? pref.getThemeMode() : null,
        pref != null ? pref.getDensity() : null,
        // convert domain types to strings for the read model
        pref != null && pref.getLocale() != null ? pref.getLocale().toLanguageTag() : null,
        pref != null && pref.getTimeZone() != null ? pref.getTimeZone().getId() : null,
        pref != null && pref.getCurrency() != null ? pref.getCurrency().getCurrencyCode() : null
    );

    // effective (server resolves)
    ThemeMode effTheme = prefDetails.themeMode() != null ? prefDetails.themeMode() : ThemeMode.SYSTEM;
    Short effDensity = prefDetails.density(); // keep null = global default OR set 0
    String effLocale = firstNonBlank(prefDetails.locale(), requestLocale, "fr");
    String effTimeZone = firstNonBlank(prefDetails.timeZone(), tenantTz, "America/Port-au-Prince");
    String effCurrency = firstNonBlank(prefDetails.currency(), tenantCurrency, "USD");

    var tenant = new TenantContext(
        tenantId,
        tenantCode,
        tenantTz,
        tenantCurrency
    );

    var effective = new EffectiveUiContext(effTheme, effDensity, effLocale, effTimeZone, effCurrency);

    return new CurrentUserDetails(
        user.getId(),
        user.getKeycloakSub(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getDisplayName(),
        tenant,
        prefDetails,
        effective
    );
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) return v.trim();
    }
    return null;
  }
}
