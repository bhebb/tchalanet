package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawApplyJdbcRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawApplyJdbcAdapter implements DrawApplyPort {

  private final DrawApplyJdbcRepository repo;

  @Override
  public ApplyOutcome attachResultAndMarkResulted(
      DrawId drawId, DrawResultId drawResultId, boolean force) {
    int updated =
        repo.attachResult(
            drawId == null ? null : drawId.uuid(),
            drawResultId == null ? null : drawResultId.uuid(),
            force);
    return updated > 0 ? ApplyOutcome.UPDATED : ApplyOutcome.ALREADY_LINKED_OR_NOT_ELIGIBLE;
  }

  @Override
  public ApplyOutcome attachResultBySlot(
      TenantId tenantId,
      LocalDate drawDate,
      ResultSlotId resultSlotId,
      DrawResultId drawResultId,
      boolean force) {
    int updated =
        repo.attachResultBySlot(
            tenantId.value(),
            drawDate,
            resultSlotId == null ? null : resultSlotId.uuid(),
            drawResultId == null ? null : drawResultId.uuid(),
            force);
    return updated > 0 ? ApplyOutcome.UPDATED : ApplyOutcome.ALREADY_LINKED_OR_NOT_ELIGIBLE;
  }
}
