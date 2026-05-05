package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import java.time.Instant;
import java.util.Optional;

public interface DrawResultReaderPort {

    DrawResult getById(DrawResultId id);

    Optional<DrawResultView> findViewById(DrawResultId id);

    Optional<DrawResultView> findViewBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt);

    Optional<DrawResultProjection> findProjectionById(DrawResultId id);

    Optional<DrawResultProjection> findProjectionBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt);

    Optional<DrawResultId> findByResultSlotIdAndOccurredAt(ResultSlotId resultSlotId, Instant occurredAt);

    TchPage<DrawResultView> findViewsByCriteria(DrawResultsCriteria criteria);

    Optional<DrawResultView> findByDrawId(DrawId drawId);
}
