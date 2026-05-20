package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;
import com.tchalanet.server.core.session.api.query.CashierSessionSummaryView;

import java.util.Optional;

public interface CashierSessionDashboardReaderPort {
    Optional<CashierSessionSummaryView> findActiveSessionSummary(TenantId tenantId, UserId cashierId);
    CashierIdentityView findIdentity(TenantId tenantId, UserId cashierId);
}
