package com.tchalanet.server.core.offlinesync.internal.domain.model.code;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;

/** Stable identity of an offline code and its owning batch / grant. */
public record CodeIdentity(
    OfflineCodeId id,
    TenantId tenantId,
    OfflineCodeBatchId codeBatchId,
    OfflineGrantId grantId,
    String code
) {
    public CodeIdentity {
        if (id == null) throw new IllegalArgumentException("id required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (codeBatchId == null) throw new IllegalArgumentException("codeBatchId required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code required");
    }
}
