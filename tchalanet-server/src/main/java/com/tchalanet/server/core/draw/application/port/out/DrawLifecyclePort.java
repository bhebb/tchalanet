package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.ExistingDrawKey;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.domain.model.Draw;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;


public interface DrawLifecyclePort {

    List<OpenableDrawRow> findOpenable(
        Instant now,
        int limit,
        int openHorizonHours,
        int openLagHours
    );

    int bulkOpen(List<DrawId> drawIds);

    List<DueToCloseRow> findDueToClose(Instant now, int limit);

    int bulkClose(List<DrawId> drawIds);

    int bulkInsert(List<NewDrawRow> rows);

    Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to);

    Draw save(Draw draw);
}
