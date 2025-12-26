package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DrawResultReaderPort {
  Optional<DrawResult> findByDrawId(TenantId tenantId, DrawId drawId);

  // éventuellement
  List<DrawResult> findByTenantAndDateRange(TenantId tenantId, LocalDate from, LocalDate to);

  List<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria);
}
