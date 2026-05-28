package com.tchalanet.server.core.reconciliation.internal.domain.service;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomalyType;
import java.util.Objects;
import java.util.UUID;

public final class ReconciliationAnomalyFingerprintPolicy {

    private ReconciliationAnomalyFingerprintPolicy() {}

    public static String fingerprint(
        ReconciliationAnomalyType type,
        DrawId drawId,
        TicketId ticketId,
        UUID payoutClaimId,
        PayoutId payoutPaymentId
    ) {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(drawId, "drawId is required");
        return type.name()
            + ":" + drawId.value()
            + ":" + value(ticketId)
            + ":" + value(payoutClaimId)
            + ":" + value(payoutPaymentId);
    }

    private static String value(TicketId id) {
        return id == null ? "" : id.value().toString();
    }

    private static String value(PayoutId id) {
        return id == null ? "" : id.value().toString();
    }

    private static String value(UUID id) {
        return id == null ? "" : id.toString();
    }
}
