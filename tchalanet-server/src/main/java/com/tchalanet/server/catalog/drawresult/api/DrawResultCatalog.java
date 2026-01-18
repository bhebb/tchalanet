package com.tchalanet.server.catalog.drawresult.api;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawResult;
import com.tchalanet.server.catalog.drawresult.internal.application.port.out.DrawResultReaderPort;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawResultCatalog {

  private final DrawResultReaderPort reader;

  @Cacheable(
      cacheNames = "drawresult.id.bySlotOccurred",
      key = "#resultSlotId.uuid().toString() + '|' + #occurredAt.getEpochSecond()")
  public Optional<DrawResultId> findByResulSlotIdAndOccurredAt(
      ResultSlotId resultSlotId, Instant occurredAt) {
    if (resultSlotId == null || occurredAt == null) return Optional.empty();
    return reader.findByResultSlotIdAndOccurredAt(resultSlotId, occurredAt);
  }

  @Cacheable(cacheNames = "drawresult.id.byId", key = "#id.uuid().toString()")
  public DrawResult getById(DrawResultId id) {
    return reader.getById(id);
  }
}
