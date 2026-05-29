package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;

import java.util.List;

public interface OfflineSubmissionLineReaderPort {

    /**
     * Hydrate persisted lines into the same {@link OfflineSubmissionLineSnapshot} format
     * used in promotionDecision events — so the recovery handler can rebuild a fresh draft.
     *
     * @param currency currency to apply on the Money fields (the line table stores plain
     *                 BigDecimals; the canonical currency lives on the parent submission).
     */
    List<OfflineSubmissionLineSnapshot> findBySubmissionId(
        OfflineSubmissionId submissionId, CurrencyCode currency);
}
