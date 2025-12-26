package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DrawLifecyclePort {
  List<OpenableDrawRow> findOpenable(Instant now, int limit, int openHorizonHours, int openLagHours);

  int bulkOpen(List<DrawId> drawIds);

  List<DueToCloseRow> findDueToClose(Instant now, int limit);

  int bulkClose(List<DrawId> drawIds);
}
