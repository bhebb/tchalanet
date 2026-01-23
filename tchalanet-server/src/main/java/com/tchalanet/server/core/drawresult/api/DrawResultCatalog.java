package com.tchalanet.server.catalog.drawresult.api;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

import java.time.Instant;
import java.util.Optional;

public interface DrawResultCatalog {

    Optional<DrawResultId> findIdByResultSlotIdAndOccurredAt(ResultSlotId resultSlotId, Instant occurredAt);

    DrawResultView getById(DrawResultId id);

    TchPage<DrawResultView> search(DrawResultsCriteria criteria, TchPageRequest pageReq);
}
