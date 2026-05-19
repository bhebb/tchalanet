package com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

/** Stable identity / operational scope of a code batch. */
public record CodeBatchIdentity(
    OfflineCodeBatchId id,
    TenantId tenantId,
    OfflineGrantId grantId,
    TerminalId terminalId,
    OutletId outletId,
    UserId sellerUserId
) {
    public CodeBatchIdentity {
        if (id == null) throw new IllegalArgumentException("id required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (grantId == null) throw new IllegalArgumentException("grantId required");
        if (terminalId == null) throw new IllegalArgumentException("terminalId required");
    }
}
