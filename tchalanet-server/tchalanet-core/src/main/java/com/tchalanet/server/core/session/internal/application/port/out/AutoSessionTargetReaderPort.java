package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionCloseTarget;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionOpenTarget;

import java.time.Instant;
import java.util.List;

public interface AutoSessionTargetReaderPort {

    List<AutoSessionOpenTarget> findDueOpenTargets(Instant now);

    List<AutoSessionCloseTarget> findDueCloseTargets(Instant now);

    List<AutoSessionCloseTarget> findOpenCloseTargetsByOutlet(
        TenantId tenantId,
        OutletId outletId,
        Instant closedAt,
        UserId closedBy,
        String reason);
}
