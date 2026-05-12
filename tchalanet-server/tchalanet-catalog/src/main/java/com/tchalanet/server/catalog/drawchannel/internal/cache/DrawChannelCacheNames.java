package com.tchalanet.server.catalog.drawchannel.internal.cache;

public final class DrawChannelCacheNames {

    private DrawChannelCacheNames() {
    }

    public static final String BY_TENANT = "catalog:drawchannel:by_tenant";
    public static final String BY_ID = "catalog:drawchannel:by_id";
    public static final String BY_TENANT_GAME_MAP = "catalog:drawchannel:by_tenant_game_map";

    // New cache names
    public static final String CALENDAR_ROWS = "catalog:drawchannel:calendar_rows";
    public static final String BY_TENANT_BY_RESULT_SLOT_ID = "catalog:drawchannel:by_tenant_by_result_slot_id";
    public static final String BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY = "catalog:drawchannel:by_tenant_by_result_slot_provider_key";
}
