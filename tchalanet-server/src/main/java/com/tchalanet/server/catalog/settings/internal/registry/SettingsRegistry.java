package com.tchalanet.server.catalog.settings.internal.registry;

import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Settings Registry (INTERNAL)
 *
 * <p>Central registry of all allowed application settings. This provides type-safe definitions and
 * validation.
 *
 * <p>This is INTERNAL and used for validation only. It is NOT exposed in the public API.
 */
public final class SettingsRegistry {

  private SettingsRegistry() {}

  // ========================================
  // POS / Session behavior
  // ========================================

  public static final SettingKeyDef<Boolean> POS_REQUIRE_OPEN_SESSION =
      new SettingKeyDef<>("pos.behavior", "require_open_session", SettingValueType.BOOLEAN, true);

  public static final SettingKeyDef<Boolean> POS_ALLOW_SALE_WITHOUT_DRAW =
      new SettingKeyDef<>(
          "pos.behavior", "allow_sale_without_draw", SettingValueType.BOOLEAN, false);

  public static final SettingKeyDef<Integer> POS_SESSION_AUTO_CLOSE_MINUTES =
      new SettingKeyDef<>("pos.session", "auto_close_minutes", SettingValueType.INT, 0);

  public static final SettingKeyDef<Integer> POS_MAX_TICKET_LINES =
      new SettingKeyDef<>("pos.ticket", "max_ticket_lines", SettingValueType.INT, 50);

  public static final SettingKeyDef<Integer> POS_MAX_SELECTIONS_PER_LINE =
      new SettingKeyDef<>("pos.ticket", "max_selections_per_line", SettingValueType.INT, 1);

  public static final SettingKeyDef<Boolean> POS_PRINT_RECEIPT_ENABLED =
      new SettingKeyDef<>("pos.receipt", "print_enabled", SettingValueType.BOOLEAN, true);

  // ========================================
  // Ticket lifecycle / verification
  // ========================================

  public static final SettingKeyDef<Integer> TICKET_PUBLIC_VISIBILITY_DAYS =
      new SettingKeyDef<>("ticket.verification", "public_visibility_days", SettingValueType.INT, 14);

  public static final SettingKeyDef<Integer> TICKET_PUBLIC_TOKEN_TTL_MINUTES =
      new SettingKeyDef<>(
          "ticket.verification", "public_token_ttl_minutes", SettingValueType.INT, 60 * 24);

  public static final SettingKeyDef<Integer> TICKET_PUBLIC_RATE_LIMIT_PER_MINUTE =
      new SettingKeyDef<>("ticket.verification", "rate_limit_per_minute", SettingValueType.INT, 60);

  public static final SettingKeyDef<Boolean> TICKET_PUBLIC_NO_INDEX =
      new SettingKeyDef<>("ticket.verification", "no_index", SettingValueType.BOOLEAN, true);

  // ========================================
  // Payout
  // ========================================

  public static final SettingKeyDef<Integer> PAYOUT_MAX_AGE_DAYS =
      new SettingKeyDef<>("payout.rules", "max_ticket_age_days", SettingValueType.INT, 30);

  public static final SettingKeyDef<Boolean> PAYOUT_REQUIRE_WON_STATUS =
      new SettingKeyDef<>("payout.rules", "require_won_status", SettingValueType.BOOLEAN, true);

  public static final SettingKeyDef<Boolean> PAYOUT_ALLOW_PARTIAL_PAYOUT =
      new SettingKeyDef<>("payout.rules", "allow_partial_payout", SettingValueType.BOOLEAN, false);

  // ========================================
  // Offline sync
  // ========================================

  public static final SettingKeyDef<Boolean> OFFLINE_ENABLED =
      new SettingKeyDef<>("offline.sync", "enabled", SettingValueType.BOOLEAN, true);

  public static final SettingKeyDef<Integer> OFFLINE_MAX_QUEUE =
      new SettingKeyDef<>("offline.sync", "max_queue", SettingValueType.INT, 500);

  public static final SettingKeyDef<Integer> OFFLINE_RETRY_BACKOFF_SECONDS =
      new SettingKeyDef<>("offline.sync", "retry_backoff_seconds", SettingValueType.INT, 15);

  // ========================================
  // Dashboard / Stats
  // ========================================

  public static final SettingKeyDef<Integer> DASHBOARD_DEFAULT_RANGE_DAYS =
      new SettingKeyDef<>("dashboard.stats", "default_range_days", SettingValueType.INT, 7);

  public static final SettingKeyDef<Integer> DASHBOARD_TOP_CASHIERS_LIMIT =
      new SettingKeyDef<>("dashboard.stats", "top_cashiers_limit", SettingValueType.INT, 10);

  public static final SettingKeyDef<Integer> DASHBOARD_CACHE_TTL_SECONDS =
      new SettingKeyDef<>("dashboard.stats", "cache_ttl_seconds", SettingValueType.INT, 60);

  // ========================================
  // UI / i18n
  // ========================================

  public static final SettingKeyDef<String> UI_DEFAULT_LOCALE =
      new SettingKeyDef<>("ui.i18n", "default_locale", SettingValueType.STRING, "fr");

  public static final SettingKeyDef<String> UI_SUPPORTED_LOCALES =
      new SettingKeyDef<>("ui.i18n", "supported_locales", SettingValueType.STRING, "fr,en,ht");

  public static final SettingKeyDef<String> UI_THEME_MODE =
      new SettingKeyDef<>("ui.theme", "mode", SettingValueType.STRING, "system");

  public static final SettingKeyDef<Integer> UI_DENSITY =
      new SettingKeyDef<>("ui.theme", "density", SettingValueType.INT, 0);

  public static final SettingKeyDef<String> UI_PUBLIC_HOME_VARIANT =
      new SettingKeyDef<>("ui.public_home", "variant", SettingValueType.STRING, "v1");

  // ========================================
  // Ops / Hours & Outlet config
  // ========================================

  public static final SettingKeyDef<String> OPS_TIMEZONE_OVERRIDE =
      new SettingKeyDef<>("ops", "timezone_override", SettingValueType.STRING, "");

  public static final SettingKeyDef<String> OPS_HOURS_JSON =
      new SettingKeyDef<>(
          "ops.hours",
          "schedule",
          SettingValueType.JSON,
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

  public static final SettingKeyDef<String> OPS_OUTLET_DAY_POLICY_JSON =
      new SettingKeyDef<>(
          "ops.outlet_day",
          "policy",
          SettingValueType.JSON,
          """
{
  "enabled": false,
  "close_requires_all_sessions_closed": true,
  "close_generates_snapshot": true,
  "close_time_local": "23:59",
  "auto_open_local": "06:00"
}
""");

  // ========================================
  // Registry API
  // ========================================

  /**
   * Get all registered setting definitions.
   *
   * @return list of all settings
   */
  public static List<SettingKeyDef<?>> all() {
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

  /**
   * Get settings indexed by full key ("namespace.key").
   *
   * @return map of fullKey → definition
   */
  public static Map<String, SettingKeyDef<?>> byFullKey() {
    return all().stream()
        .collect(Collectors.toUnmodifiableMap(SettingKeyDef::fullKey, k -> k));
  }
}
