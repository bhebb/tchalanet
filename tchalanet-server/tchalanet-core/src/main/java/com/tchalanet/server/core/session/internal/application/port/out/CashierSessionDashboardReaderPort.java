package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;

public interface CashierSessionDashboardReaderPort {
    CashierIdentityView findIdentity(TenantId tenantId, UserId cashierId);
}
