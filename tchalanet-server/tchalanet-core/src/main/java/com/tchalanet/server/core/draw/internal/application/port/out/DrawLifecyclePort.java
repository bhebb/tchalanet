package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.query.ExistingDrawKey;
import com.tchalanet.server.core.draw.api.query.NewDrawRow;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import com.tchalanet.server.core.draw.internal.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface DrawLifecyclePort {

  List<OpenableDrawRow> findOpenable(
      Instant now, int limit, int openHorizonHours, int openLagHours);

  int bulkOpen(List<DrawId> drawIds, Instant now);

  /**
   * Cancel still-SCHEDULED draws (e.g. provider became unavailable for that day). Guarded to {@code
   * status='SCHEDULED'} so it is idempotent and never touches draws that already opened or have
   * sales.
   *
   * @return number of rows transitioned to CANCELED.
   */
  int bulkCancelScheduled(List<DrawId> drawIds, String reasonCode, String reasonLabel, Instant now);

  List<DueToCloseRow> findDueToClose(Instant now, int limit);

  int bulkClose(List<DrawId> drawIds, Instant now);

  int bulkInsert(List<NewDrawRow> rows);

  Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to);

  Draw save(Draw draw);
}
