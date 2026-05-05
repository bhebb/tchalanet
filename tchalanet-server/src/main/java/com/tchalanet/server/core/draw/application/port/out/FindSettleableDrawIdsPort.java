package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;

public interface FindSettleableDrawIdsPort {

    record SettleableDrawCriteria(
        TenantId tenantId,
        Instant from,
        Instant to,
        long maxDraws,
        boolean force
    ) {}

    List<DrawId> findSettleableDrawIds(SettleableDrawCriteria criteria);
}
