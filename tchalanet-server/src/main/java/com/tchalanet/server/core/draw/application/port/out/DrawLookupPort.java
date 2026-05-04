package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummaryView;
import com.tchalanet.server.core.draw.domain.model.Draw;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DrawLookupPort {

    Optional<Draw> findById(DrawId drawId);
    Draw getById(DrawId drawId);

    Optional<DrawId> findDrawIdBySlotId(
        TenantId tenantId,
        LocalDate drawDate,
        ResultSlotId resultSlotId
    );

    List<DrawSummaryView> findByCriteria(DrawSearchCriteria criteria);

    boolean existsSettledDrawForResult(DrawResultId drawResultId);

    List<DrawSummaryView> findByDrawResultId(DrawResultId drawResultId);

    List<DrawSummaryView> findResultedWithProvisionalOlderThan(Duration duration);
}
