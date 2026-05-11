package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.SalesSessionOperation;

public record ValidateSalesSessionForOperationQuery(
    TenantId tenantId,
    SalesSessionId salesSessionId,
    TerminalId terminalId,
    OutletId outletId,
    UserId sellerUserId,
    SalesSessionOperation operation
) implements Query<ValidatedSalesSessionOperationView> {}
