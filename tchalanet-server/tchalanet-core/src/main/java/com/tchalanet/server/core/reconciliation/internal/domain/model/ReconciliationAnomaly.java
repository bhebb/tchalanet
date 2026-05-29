package com.tchalanet.server.core.reconciliation.internal.domain.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.ReconciliationAnomalyId;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ReconciliationAnomaly(
    ReconciliationAnomalyId id,
    TenantId tenantId,
    ReconciliationRunId runId,
    LocalDate businessDate,
    ReconciliationSeverity severity,
    ReconciliationAnomalyType anomalyType,
    ReconciliationAnomalyStatus status,
    String fingerprint,
    DrawId drawId,
    DrawChannelId drawChannelId,
    DrawResultId drawResultId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    UUID payoutClaimId,
    PayoutId payoutPaymentId,
    String expectedStatus,
    String actualStatus,
    BigDecimal expectedAmount,
    BigDecimal actualAmount,
    String currency,
    String message,
    String detailsJson,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant resolvedAt
) {
    public ReconciliationAnomaly {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(runId, "runId is required");
        Objects.requireNonNull(businessDate, "businessDate is required");
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(anomalyType, "anomalyType is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(fingerprint, "fingerprint is required");
        Objects.requireNonNull(message, "message is required");
        firstSeenAt = firstSeenAt == null ? Instant.now() : firstSeenAt;
        lastSeenAt = lastSeenAt == null ? firstSeenAt : lastSeenAt;
        detailsJson = detailsJson == null || detailsJson.isBlank() ? "{}" : detailsJson;
    }
}
