package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawApplyJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

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
        Instant now) {

        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(drawDate, "drawDate is required");
        Objects.requireNonNull(resultSlotId, "resultSlotId is required");
        Objects.requireNonNull(drawResultId, "drawResultId is required");
        Objects.requireNonNull(now, "now is required");

        var rows = repo.attachResultBySlotReturning(
            tenantId.value(),
            drawDate,
            resultSlotId.value(),
            drawResultId.value(),
            now);

        if (rows == null || rows.isEmpty()) {
            return ApplyResult.none(ApplyOutcome.ALREADY_LINKED_OR_NOT_ELIGIBLE);
        }

        var applied = rows.stream()
            .map(r -> new AppliedDraw(
                DrawId.of(r.drawId()),
                DrawChannelId.of(r.drawChannelId())))
            .toList();

        return ApplyResult.updated(applied);
    }

}
