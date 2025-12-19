package com.tchalanet.server.common.settings.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.settings.dto.ResolvedSettingDto;

import java.util.List;
import java.util.UUID;

public record ResolveAppSettingsQuery(
    UUID tenantId,
    UUID outletId,     // nullable
    UUID terminalId,   // nullable
    List<String> namespaces
) implements Query<List<ResolvedSettingDto>> {
}
