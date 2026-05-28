package com.tchalanet.server.core.reconciliation.internal.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaEntity;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@UseCase
public class ReconciliationAnomalyCsvExporter {

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ReconciliationAnomalyJpaRepository anomalies;

    public ReconciliationAnomalyCsvExporter(ReconciliationAnomalyJpaRepository anomalies) {
        this.anomalies = anomalies;
    }

    public byte[] export(TenantId tenantId, String tenantCode, ReconciliationRunId runId) {
        var rows = anomalies.findByTenantIdAndRunIdOrderBySeverityAscAnomalyTypeAsc(
            tenantId.value(),
            runId.value());
        var csv = new StringBuilder();
        appendRow(csv, List.of(
            "run_id", "tenant_id", "tenant_code", "business_date", "severity", "anomaly_type",
            "anomaly_status", "draw_id", "draw_channel_id", "draw_result_id", "ticket_id",
            "ticket_code", "public_code", "display_code", "payout_claim_id", "payout_payment_id",
            "expected_status", "actual_status", "expected_amount", "actual_amount", "currency",
            "message", "fingerprint", "created_at"));
        rows.forEach(row -> appendRow(csv, Arrays.asList(
            string(row.getRunId()),
            string(row.getTenantId()),
            tenantCode,
            string(row.getBusinessDate()),
            row.getSeverity().name(),
            row.getAnomalyType().name(),
            row.getStatus().name(),
            string(row.getDrawId()),
            string(row.getDrawChannelId()),
            string(row.getDrawResultId()),
            string(row.getTicketId()),
            row.getTicketCode(),
            row.getPublicCode(),
            row.getDisplayCode(),
            string(row.getPayoutClaimId()),
            string(row.getPayoutPaymentId()),
            row.getExpectedStatus(),
            row.getActualStatus(),
            amount(row.getExpectedAmount()),
            amount(row.getActualAmount()),
            row.getCurrency(),
            row.getMessage(),
            row.getFingerprint(),
            instant(row.getCreatedAt() == null ? row.getFirstSeenAt() : row.getCreatedAt())
        )));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder csv, List<String> values) {
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private String escape(String value) {
        var safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String amount(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String instant(Instant value) {
        return value == null ? "" : UTC_FORMATTER.format(value.atOffset(ZoneOffset.UTC));
    }
}
