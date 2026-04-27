package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;

import java.time.Instant;
import java.util.Optional;

/**
 * Public application port for reading DrawResults.
 */
public interface DrawResultReaderPort {

    DrawResult getById(DrawResultId id);

    TchPage<DrawResult> findByCriteria(DrawResultsCriteria criteria);

    /**
     * Lookup drawResult id by resultSlotId + occurredAt. Returns Optional.empty() when not found.
     * This method is used by cross-module handlers that need to attach a published draw result to a draw.
     */
    Optional<DrawResultId> findByResultSlotIdAndOccurredAt(ResultSlotId resultSlotId, Instant occurredAt);
}
