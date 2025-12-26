package com.tchalanet.server.common.settings.registry;

import com.tchalanet.server.common.settings.AppSettingValueType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AppSettingRegistry {

  private AppSettingRegistry() {}

  // ----------------------------
  // POS / Session behavior
  // ----------------------------
  public static final AppSettingKey<Boolean> POS_REQUIRE_OPEN_SESSION =
      new AppSettingKey<>(
          "pos.behavior", "require_open_session", AppSettingValueType.BOOLEAN, true);

  public static final AppSettingKey<Boolean> POS_ALLOW_SALE_WITHOUT_DRAW =
      new AppSettingKey<>(
          "pos.behavior", "allow_sale_without_draw", AppSettingValueType.BOOLEAN, false);

  public static final AppSettingKey<Integer> POS_SESSION_AUTO_CLOSE_MINUTES =
      new AppSettingKey<>("pos.session", "auto_close_minutes", AppSettingValueType.INT, 0);
  // 0 = disabled (safe default)

  public static final AppSettingKey<Integer> POS_MAX_TICKET_LINES =
      new AppSettingKey<>("pos.ticket", "max_ticket_lines", AppSettingValueType.INT, 50);

  public static final AppSettingKey<Integer> POS_MAX_SELECTIONS_PER_LINE =
      new AppSettingKey<>("pos.ticket", "max_selections_per_line", AppSettingValueType.INT, 1);
  // borlette “classic” = 1 sélection par ligne (tu pourras évoluer)

  public static final AppSettingKey<Boolean> POS_PRINT_RECEIPT_ENABLED =
      new AppSettingKey<>("pos.receipt", "print_enabled", AppSettingValueType.BOOLEAN, true);

  // ----------------------------
  // Ticket lifecycle / verification public
  // ----------------------------
  public static final AppSettingKey<Integer> TICKET_PUBLIC_VISIBILITY_DAYS =
      new AppSettingKey<>(
          "ticket.verification", "public_visibility_days", AppSettingValueType.INT, 14);
  // fenêtre de visibilité /ticket/:code (7–30 typique)

  public static final AppSettingKey<Integer> TICKET_PUBLIC_TOKEN_TTL_MINUTES =
      new AppSettingKey<>(
          "ticket.verification", "public_token_ttl_minutes", AppSettingValueType.INT, 60 * 24);
  // TTL token signé (1 jour)

  public static final AppSettingKey<Integer> TICKET_PUBLIC_RATE_LIMIT_PER_MINUTE =
      new AppSettingKey<>(
          "ticket.verification", "rate_limit_per_minute", AppSettingValueType.INT, 60);

  public static final AppSettingKey<Boolean> TICKET_PUBLIC_NO_INDEX =
      new AppSettingKey<>("ticket.verification", "no_index", AppSettingValueType.BOOLEAN, true);

  // ----------------------------
  // Payout (paiement)
  // ----------------------------
  public static final AppSettingKey<Integer> PAYOUT_MAX_AGE_DAYS =
      new AppSettingKey<>("payout.rules", "max_ticket_age_days", AppSettingValueType.INT, 30);

  public static final AppSettingKey<Boolean> PAYOUT_REQUIRE_WON_STATUS =
      new AppSettingKey<>("payout.rules", "require_won_status", AppSettingValueType.BOOLEAN, true);

  public static final AppSettingKey<Boolean> PAYOUT_ALLOW_PARTIAL_PAYOUT =
      new AppSettingKey<>(
          "payout.rules", "allow_partial_payout", AppSettingValueType.BOOLEAN, false);

  // ----------------------------
  // Offline sync
  // ----------------------------
  public static final AppSettingKey<Boolean> OFFLINE_ENABLED =
      new AppSettingKey<>("offline.sync", "enabled", AppSettingValueType.BOOLEAN, true);

  public static final AppSettingKey<Integer> OFFLINE_MAX_QUEUE =
      new AppSettingKey<>("offline.sync", "max_queue", AppSettingValueType.INT, 500);

  public static final AppSettingKey<Integer> OFFLINE_RETRY_BACKOFF_SECONDS =
      new AppSettingKey<>("offline.sync", "retry_backoff_seconds", AppSettingValueType.INT, 15);

  // ----------------------------
  // Dashboard / Stats
  // ----------------------------
  public static final AppSettingKey<Integer> DASHBOARD_DEFAULT_RANGE_DAYS =
      new AppSettingKey<>("dashboard.stats", "default_range_days", AppSettingValueType.INT, 7);

  public static final AppSettingKey<Integer> DASHBOARD_TOP_CASHIERS_LIMIT =
      new AppSettingKey<>("dashboard.stats", "top_cashiers_limit", AppSettingValueType.INT, 10);

  public static final AppSettingKey<Integer> DASHBOARD_CACHE_TTL_SECONDS =
      new AppSettingKey<>("dashboard.stats", "cache_ttl_seconds", AppSettingValueType.INT, 60);
  // utile si tu caches les réponses BFF/dashboard en plus du cache SQL

  // ----------------------------
  // UI / i18n (minimum)
  // ----------------------------
  public static final AppSettingKey<String> UI_DEFAULT_LOCALE =
      new AppSettingKey<>("ui.i18n", "default_locale", AppSettingValueType.STRING, "fr");

  public static final AppSettingKey<String> UI_SUPPORTED_LOCALES =
      new AppSettingKey<>("ui.i18n", "supported_locales", AppSettingValueType.STRING, "fr,en,ht");

  public static final AppSettingKey<String> UI_THEME_MODE =
      new AppSettingKey<>("ui.theme", "mode", AppSettingValueType.STRING, "system");
  // system|light|dark

  public static final AppSettingKey<Integer> UI_DENSITY =
      new AppSettingKey<>("ui.theme", "density", AppSettingValueType.INT, 0);

  public static final AppSettingKey<String> UI_PUBLIC_HOME_VARIANT =
      new AppSettingKey<>("ui.public_home", "variant", AppSettingValueType.STRING, "v1");

  // ----------------------------
  // Ops / Hours & Outlet config
  // ----------------------------
  public static final AppSettingKey<String> OPS_TIMEZONE_OVERRIDE =
      new AppSettingKey<>("ops", "timezone_override", AppSettingValueType.STRING, "");
  // si vide => tenant.timezone (table tenant)

  public static final AppSettingKey<String> OPS_HOURS_JSON =
      new AppSettingKey<>(
          "ops.hours",
          "schedule",
          AppSettingValueType.JSON,
"""
{
  "timezone": "",
  "week": {
    "mon": [{"open":"08:00","close":"20:00"}],
    "tue": [{"open":"08:00","close":"20:00"}],
    "wed": [{"open":"08:00","close":"20:00"}],
    "thu": [{"open":"08:00","close":"20:00"}],
    "fri": [{"open":"08:00","close":"20:00"}],
    "sat": [{"open":"09:00","close":"18:00"}],
    "sun": []
  },
  "holidays": [],
  "exceptions": []
}
""");

  public static final AppSettingKey<String> OPS_OUTLET_DAY_POLICY_JSON =
      new AppSettingKey<>(
          "ops.outlet_day",
          "policy",
          AppSettingValueType.JSON,
"""
{
  "enabled": false,
  "close_requires_all_sessions_closed": true,
  "close_generates_snapshot": true,
  "close_time_local": "23:59",
  "auto_open_local": "06:00"
}
""");

  // ----------------------------
  // Registry API
  // ----------------------------
  public static List<AppSettingKey<?>> all() {
    return List.of(
        POS_REQUIRE_OPEN_SESSION,
        POS_ALLOW_SALE_WITHOUT_DRAW,
        POS_SESSION_AUTO_CLOSE_MINUTES,
        POS_MAX_TICKET_LINES,
        POS_MAX_SELECTIONS_PER_LINE,
        POS_PRINT_RECEIPT_ENABLED,
        TICKET_PUBLIC_VISIBILITY_DAYS,
        TICKET_PUBLIC_TOKEN_TTL_MINUTES,
        TICKET_PUBLIC_RATE_LIMIT_PER_MINUTE,
        TICKET_PUBLIC_NO_INDEX,
        PAYOUT_MAX_AGE_DAYS,
        PAYOUT_REQUIRE_WON_STATUS,
        PAYOUT_ALLOW_PARTIAL_PAYOUT,
        OFFLINE_ENABLED,
        OFFLINE_MAX_QUEUE,
        OFFLINE_RETRY_BACKOFF_SECONDS,
        DASHBOARD_DEFAULT_RANGE_DAYS,
        DASHBOARD_TOP_CASHIERS_LIMIT,
        DASHBOARD_CACHE_TTL_SECONDS,
        UI_DEFAULT_LOCALE,
        UI_SUPPORTED_LOCALES,
        UI_THEME_MODE,
        UI_DENSITY,
        UI_PUBLIC_HOME_VARIANT,
        OPS_TIMEZONE_OVERRIDE,
        OPS_HOURS_JSON,
        OPS_OUTLET_DAY_POLICY_JSON);
  }

  public static Map<String, AppSettingKey<?>> byFullKey() {
    return all().stream().collect(Collectors.toUnmodifiableMap(AppSettingKey::fullKey, k -> k));
  }
}
