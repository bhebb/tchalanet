package com.tchalanet.server.core.draw.application.port.out;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawReaderPort {
  Optional<Draw> findById(DrawId drawId);

  List<Draw> findClosableDraws(TenantId tenantId, ZonedDateTime now);

  List<Draw> findResultedUnsettled(TenantId tenantId, ZonedDateTime now);

  Optional<Draw> findNext(GetNextDrawQuery query);

  List<DrawSummary> findByCriteria(DrawSearchCriteria drawSearchCriteria);

  List<Draw> findNextForChannels(GetNextDrawsQuery query);
}
