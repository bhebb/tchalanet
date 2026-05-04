package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DrawApplyPort {

    enum ApplyOutcome {
        UPDATED,
        ALREADY_LINKED_OR_NOT_ELIGIBLE
    }

    record AppliedDraw(DrawId drawId, DrawChannelId drawChannelId) {
    }

    record ApplyResult(ApplyOutcome outcome, List<AppliedDraw> applied) {
        public static ApplyResult none(ApplyOutcome outcome) {
            return new ApplyResult(outcome, List.of());
        }

        public static ApplyResult updated(List<AppliedDraw> applied) {
            return new ApplyResult(ApplyOutcome.UPDATED, applied == null ? List.of() : List.copyOf(applied));
        }
    }

    ApplyResult attachResultBySlot(
        TenantId tenantId,
        LocalDate drawDate,
        ResultSlotId resultSlotId,
        DrawResultId drawResultId,
        Instant now,
        boolean force);
}
