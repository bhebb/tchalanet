package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Optional;

public interface AdminPosSelectionLookup {

    Optional<AdminPosSelection> findActive(TenantId tenantId, UserId userId);
}
