package com.tchalanet.server.core.settings.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.settings.dto.ResolvedSettingDto;
import java.util.List;

public record ResolveAppSettingsQuery(
    TenantId tenantId,
    OutletId outletId, // nullable
    TerminalId terminalId, // nullable
    List<String> namespaces)
    implements Query<List<ResolvedSettingDto>> {}
