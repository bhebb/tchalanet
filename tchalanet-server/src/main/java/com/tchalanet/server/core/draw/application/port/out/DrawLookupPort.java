package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawLookupPort {
  Optional<Draw> findById(DrawId drawId);

  List<DrawSummary> findByCriteria(DrawSearchCriteria drawSearchCriteria);

  Optional<DrawId> findDrawIdBySlotId(TenantId tenantId, LocalDate drawDate, ResultSlotId resultSlotId);
}
