package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public interface DrawApplyPort {
  enum ApplyOutcome {
    UPDATED,
    ALREADY_LINKED_OR_NOT_ELIGIBLE
  }

  ApplyOutcome attachResultAndMarkResulted(DrawId drawId, DrawResultId drawResultId, boolean force);

  ApplyOutcome attachResultBySlot(
      TenantId tenantId,
      LocalDate drawDate,
      ResultSlotId resultSlotId,
      DrawResultId drawResultId,
      boolean force);
}
