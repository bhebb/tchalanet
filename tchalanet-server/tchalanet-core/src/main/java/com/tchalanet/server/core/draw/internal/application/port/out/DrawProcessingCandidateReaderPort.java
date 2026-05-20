package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DrawProcessingCandidateReaderPort {

    boolean hasApplyCandidates(List<DrawProcessingSlotDate> candidates);

    boolean hasSettleCandidates(List<DrawProcessingSlotDate> candidates);

    record DrawProcessingSlotDate(
        ResultSlotId resultSlotId,
        String slotKey,
        LocalDate drawDate,
        Instant expectedOccurredAt) {
    }

}
