package com.tchalanet.server.common.types.id;

public final class TypedIdRegistry {
    private TypedIdRegistry() {
    }

    /**
     * Single source of truth for all typed id wrappers that support static parse(String).
     * <p>
     * Keep this list sorted alphabetically to reduce merge conflicts.
     */
    public static final Class<?>[] ALL = {
        // --- Add all your *Id here (alphabetical) ---
        AddressId.class,
        LedgerEntryId.class,
        DrawChannelGameId.class,
        DrawChannelId.class,
        DrawId.class,
        DrawResultId.class,
        GameId.class,
        I18nOverrideId.class,
        PageModelTemplateId.class,
        PayoutId.class,
        PlanId.class,
        ResultSlotId.class,
        RoleId.class,
        SalesSessionId.class,
        SettingId.class,
        SubscriptionId.class,
        TchalaEntryId.class,
        TenantGameId.class,
        TenantId.class,
        TerminalId.class,
        ThemePresetId.class,
        TicketId.class,
        UserId.class,
        OfflineBatchId.class,
        OfflineCodeBatchId.class,
        OfflineCodeReservationId.class,
        OfflineSaleSubmissionId.class,
        OfflineSalesGrantId.class,
        OfflineTicketId.class,
    };
}
