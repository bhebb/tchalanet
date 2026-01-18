package com.tchalanet.server.catalog.drawresult.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.catalog.drawresult.api.DrawResultsSearchCriteria;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawResult;
import java.time.Instant;
import java.util.Optional;

public interface DrawResultReaderPort {
  Optional<DrawResultId> findByResultSlotIdAndOccurredAt(
      ResultSlotId resultSlotId, Instant occurredAt);

  DrawResult getById(DrawResultId id);

  TchPage<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria);
}
