package com.tchalanet.server.draw.application.port.out;

import com.tchalanet.server.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.draw.domain.model.DrawResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawResultReaderPort {
  Optional<DrawResult> findByDrawId(UUID tenantId, UUID drawId);

  // éventuellement
  List<DrawResult> findByTenantAndDateRange(UUID tenantId, LocalDate from, LocalDate to);

  List<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria);
}
