package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawApplyJdbcRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawApplyJdbcAdapter implements DrawApplyPort {

  private final DrawApplyJdbcRepository repo;

  @Override
  public ApplyResult attachResultBySlot(
      TenantId tenantId,
      LocalDate drawDate,
      ResultSlotId resultSlotId,
      DrawResultId drawResultId,
      String resultSource,
      Instant now) {

    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(drawDate, "drawDate is required");
    Objects.requireNonNull(resultSlotId, "resultSlotId is required");
    Objects.requireNonNull(drawResultId, "drawResultId is required");
    Objects.requireNonNull(resultSource, "resultSource is required");
    Objects.requireNonNull(now, "now is required");

    var rows =
        repo.attachResultBySlotReturning(
            tenantId.value(),
            drawDate,
            resultSlotId.value(),
            drawResultId.value(),
            resultSource,
            now);

    if (rows == null || rows.isEmpty()) {
      return ApplyResult.none(ApplyOutcome.ALREADY_LINKED_OR_NOT_ELIGIBLE);
    }

    var applied =
        rows.stream()
            .map(r -> new AppliedDraw(DrawId.of(r.drawId()), DrawChannelId.of(r.drawChannelId())))
            .toList();

    return ApplyResult.updated(applied);
  }
}
