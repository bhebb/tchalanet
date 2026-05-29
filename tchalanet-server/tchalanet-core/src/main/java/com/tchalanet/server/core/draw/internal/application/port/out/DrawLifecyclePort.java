package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.api.query.ExistingDrawKey;
import com.tchalanet.server.core.draw.api.query.NewDrawRow;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;


public interface DrawLifecyclePort {

    List<OpenableDrawRow> findOpenable(
        Instant now,
        int limit,
        int openHorizonHours,
        int openLagHours
    );

    List<OpenableDrawRow> findOpenableForSalesOpenTime(Instant now, LocalDate drawDate, LocalTime defaultSalesOpenTime, int limit);

    int bulkOpen(List<DrawId> drawIds, Instant now);

    List<DueToCloseRow> findDueToClose(Instant now, int limit);

    int bulkClose(List<DrawId> drawIds, Instant now);

    int bulkInsert(List<NewDrawRow> rows);

    Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to);

    Draw save(Draw draw);
}
