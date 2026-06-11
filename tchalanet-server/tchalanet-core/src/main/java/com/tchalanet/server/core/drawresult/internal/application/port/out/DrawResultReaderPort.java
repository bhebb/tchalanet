package com.tchalanet.server.core.drawresult.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface DrawResultReaderPort {

    DrawResult getById(DrawResultId id);

    Optional<DrawResultView> findViewById(DrawResultId id);

    Optional<DrawResultView> findViewBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt);

    Optional<DrawResultProjection> findProjectionById(DrawResultId id);

    Optional<DrawResultProjection> findProjectionBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt);

    Optional<DrawResultId> findByResultSlotIdAndOccurredAt(ResultSlotId resultSlotId, Instant occurredAt);

    Optional<DrawResultId> findByResultSlotIdAndResultDate(ResultSlotId resultSlotId, LocalDate resultDate);

    TchPage<DrawResultView> findViewsByCriteria(DrawResultsCriteria criteria);

    Optional<DrawResultView> findByDrawId(DrawId drawId);

    boolean existsUsableExternalResult(ResultSlotId resultSlotId, LocalDate resultDate);

}
