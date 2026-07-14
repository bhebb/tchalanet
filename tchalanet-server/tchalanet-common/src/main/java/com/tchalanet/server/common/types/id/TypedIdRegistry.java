package com.tchalanet.server.common.types.id;

public final class TypedIdRegistry {
  private TypedIdRegistry() {}

  /**
   * Single source of truth for all typed id wrappers that support static parse(String).
   *
   * <p>Keep this list sorted alphabetically to reduce merge conflicts.
   */
  public static Class<?>[] all() {
    return ENTRIES.clone();
  }

  private static final Class<?>[] ENTRIES = {
    AddressId.class,
    BusinessDayOverrideId.class,
    DrawChannelGameId.class,
    DrawChannelId.class,
    DrawId.class,
    DrawResultId.class,
    GameId.class,
    I18nOverrideId.class,
    PageModelTemplateId.class,
    PlanId.class,
    PromotionAttemptId.class,
    PromotionRuleId.class, // Added PromotionRuleId
    ResultSlotCalendarOverrideId.class,
    ResultSlotId.class,
    RoleId.class,
    SettingId.class,
    SubscriptionId.class,
    TchalaEntryId.class,
    TenantGameId.class,
    TenantId.class,
    ThemePresetId.class,
    TicketId.class,
    UserId.class,
  };
}
