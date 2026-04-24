package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawApplyJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        Instant now,
        boolean force) {

        List<DrawApplyJdbcRepository.AppliedRow> rows =
            repo.attachResultBySlotReturning(
                tenantId.value(),
                drawDate,
                resultSlotId == null ? null : resultSlotId.value(),
                drawResultId == null ? null : drawResultId.value(),
                now,
                force);

        if (rows == null || rows.isEmpty()) return ApplyResult.none();

        var applied =
            rows.stream()
                .map(r -> new AppliedDraw(DrawId.of(r.drawId()), DrawChannelId.of(r.drawChannelId())))
                .toList();

        return ApplyResult.updated(applied);
    }
}
