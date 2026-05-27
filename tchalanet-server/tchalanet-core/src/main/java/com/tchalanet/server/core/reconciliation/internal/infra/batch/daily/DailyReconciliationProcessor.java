package com.tchalanet.server.core.reconciliation.internal.infra.batch.daily;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.ReconciliationAnomalyId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.api.query.reconciliation.GetPayoutSummaryForDrawQuery;
import com.tchalanet.server.core.payout.api.query.reconciliation.ListPayoutClaimsForDrawQuery;
import com.tchalanet.server.core.payout.api.query.reconciliation.ListPayoutPaymentsForDrawQuery;
import com.tchalanet.server.core.payout.api.query.reconciliation.PayoutClaimForDrawRow;
import com.tchalanet.server.core.payout.api.query.reconciliation.PayoutPaymentForDrawRow;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomaly;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomalyStatus;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomalyType;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationSeverity;
import com.tchalanet.server.core.reconciliation.internal.domain.service.ReconciliationAnomalyFingerprintPolicy;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.query.reconciliation.ActualTicketStateRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.ExpectedTicketOutcomeRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.GetSalesOutcomeSummaryForDrawQuery;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListActualTicketStatesForDrawQuery;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListExpectedTicketOutcomesForDrawResultQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DailyReconciliationProcessor {

    private final IdGenerator idGenerator;
    private final QueryBus queryBus;

    public DailyReconciliationProcessor(IdGenerator idGenerator, QueryBus queryBus) {
        this.idGenerator = idGenerator;
        this.queryBus = queryBus;
    }

    public DailyReconciliationProcessingResult process(DailyReconciliationItem item, Instant now) {
        var drawResult = item.drawResult();
        var drawId = drawResult.drawId();
        var expectedRows = queryBus.ask(
                new ListExpectedTicketOutcomesForDrawResultQuery(drawResult.drawResultId()))
            .stream()
            .filter(row -> row.drawId().equals(drawId))
            .toList();
        var actualRows = queryBus.ask(new ListActualTicketStatesForDrawQuery(drawId));
        var claims = queryBus.ask(new ListPayoutClaimsForDrawQuery(drawId));
        var payments = queryBus.ask(new ListPayoutPaymentsForDrawQuery(drawId));
        var salesSummary = queryBus.ask(new GetSalesOutcomeSummaryForDrawQuery(drawId));
        var payoutSummary = queryBus.ask(new GetPayoutSummaryForDrawQuery(drawId));

        var counters = DailyReconciliationCounters.empty();
        counters.incrementCheckedDrawCount();
        counters.addCheckedTicketCount(actualRows.size());

        var anomalies = new ArrayList<ReconciliationAnomaly>();
        var actualByTicket = actualRows.stream()
            .collect(Collectors.toMap(ActualTicketStateRow::ticketId, Function.identity()));
        var expectedByTicket = expectedRows.stream()
            .collect(Collectors.toMap(ExpectedTicketOutcomeRow::ticketId, Function.identity()));
        var claimsByTicket = claims.stream()
            .collect(Collectors.groupingBy(PayoutClaimForDrawRow::ticketId));
        var paymentsByTicket = payments.stream()
            .collect(Collectors.groupingBy(PayoutPaymentForDrawRow::ticketId));

        for (var expected : expectedRows) {
            var actual = actualByTicket.get(expected.ticketId());
            if (isMissingResult(actual)) {
                add(anomalies, counters, anomaly(item, expected, actual, null, null,
                    ReconciliationAnomalyType.TICKET_RESULT_STATUS_MISSING_AFTER_DRAW_RESULT,
                    ReconciliationSeverity.HIGH,
                    "Ticket result status is missing after draw result application", now));
                continue;
            }
            if (expected.shouldWin() && actual.resultStatus() != TicketResultStatus.WON) {
                add(anomalies, counters, anomaly(item, expected, actual, null, null,
                    ReconciliationAnomalyType.EXPECTED_WINNER_NOT_RESULTED,
                    ReconciliationSeverity.HIGH,
                    "Expected winning ticket is not marked as won", now));
            }
            if (!expected.shouldWin() && actual.resultStatus() == TicketResultStatus.WON) {
                add(anomalies, counters, anomaly(item, expected, actual, null, null,
                    ReconciliationAnomalyType.FALSE_WINNER_RESULTED,
                    ReconciliationSeverity.CRITICAL,
                    "Non-winning ticket is marked as won", now));
            }
            if (expected.expectedPayoutAmount() != null
                && actual.actualPotentialPayout() != null
                && actual.actualPotentialPayout().amount().compareTo(expected.expectedPayoutAmount()) != 0) {
                add(anomalies, counters, anomaly(item, expected, actual, null, null,
                    ReconciliationAnomalyType.SALES_OUTCOME_AMOUNT_MISMATCH,
                    ReconciliationSeverity.HIGH,
                    "Sales outcome amount differs from expected draw outcome", now));
            }
        }

        for (var expected : expectedRows.stream()
            .filter(ExpectedTicketOutcomeRow::shouldWin)
            .filter(row -> positive(row.expectedPayoutAmount()))
            .toList()) {
            if (!claimsByTicket.containsKey(expected.ticketId())) {
                add(anomalies, counters, anomaly(item, expected, actualByTicket.get(expected.ticketId()), null, null,
                    ReconciliationAnomalyType.WINNER_WITHOUT_PAYOUT_CLAIM,
                    ReconciliationSeverity.MEDIUM,
                    "Winning ticket has no payout claim", now));
            }
        }

        for (var claim : claims) {
            var expected = expectedByTicket.get(claim.ticketId());
            if (expected == null || !expected.shouldWin()) {
                add(anomalies, counters, anomaly(item, expected, actualByTicket.get(claim.ticketId()), claim.payoutId(), null,
                    ReconciliationAnomalyType.CLAIM_FOR_NON_WINNING_TICKET,
                    ReconciliationSeverity.CRITICAL,
                    "Payout claim exists for a non-winning ticket", now));
                continue;
            }
            if (claim.amount() != null
                && expected.expectedPayoutAmount() != null
                && claim.amount().amount().compareTo(expected.expectedPayoutAmount()) != 0) {
                add(anomalies, counters, anomaly(item, expected, actualByTicket.get(claim.ticketId()), claim.payoutId(), null,
                    ReconciliationAnomalyType.PAYOUT_CLAIM_AMOUNT_MISMATCH,
                    ReconciliationSeverity.HIGH,
                    "Payout claim amount differs from expected payout amount", now));
            }
        }

        for (var payment : payments) {
            var expected = expectedByTicket.get(payment.ticketId());
            var ticketClaims = claimsByTicket.getOrDefault(payment.ticketId(), List.of());
            if (expected == null || !expected.shouldWin()) {
                add(anomalies, counters, anomaly(item, expected, actualByTicket.get(payment.ticketId()), payment.payoutId(), payment.payoutId(),
                    ReconciliationAnomalyType.PAID_NON_WINNING_TICKET,
                    ReconciliationSeverity.CRITICAL,
                    "Payment exists for a non-winning ticket", now));
            }
            ticketClaims.stream()
                .filter(claim -> payment.amount() != null
                    && claim.amount() != null
                    && payment.amount().amount().compareTo(claim.amount().amount()) > 0)
                .findFirst()
                .ifPresent(claim -> add(anomalies, counters, anomaly(item, expected, actualByTicket.get(payment.ticketId()), claim.payoutId(), payment.payoutId(),
                    ReconciliationAnomalyType.PAYMENT_EXCEEDS_CLAIM_AMOUNT,
                    ReconciliationSeverity.HIGH,
                    "Payment amount exceeds claim amount", now)));
            if (ticketClaims.stream().noneMatch(this::allowsPayment)) {
                add(anomalies, counters, anomaly(item, expected, actualByTicket.get(payment.ticketId()), payment.payoutId(), payment.payoutId(),
                    ReconciliationAnomalyType.PAYMENT_CLAIM_STATUS_MISMATCH,
                    ReconciliationSeverity.HIGH,
                    "Payment exists without an approved or paid claim", now));
            }
        }

        var expectedWinnerCount = expectedRows.stream().filter(ExpectedTicketOutcomeRow::shouldWin).count();
        if (expectedWinnerCount != salesSummary.wonTicketCount()) {
            add(anomalies, counters, anomaly(item, null, null, null, null,
                ReconciliationAnomalyType.DRAW_WINNER_COUNT_MISMATCH,
                ReconciliationSeverity.HIGH,
                "Draw winner count differs from expected outcomes", now));
        }

        var expectedPayoutTotal = expectedRows.stream()
            .map(ExpectedTicketOutcomeRow::expectedPayoutAmount)
            .filter(DailyReconciliationProcessor::positive)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (salesSummary.totalWinningAmount() != null
            && salesSummary.totalWinningAmount().amount().compareTo(expectedPayoutTotal) != 0) {
            add(anomalies, counters, anomaly(item, null, null, null, null,
                ReconciliationAnomalyType.DRAW_PAYOUT_TOTAL_MISMATCH,
                ReconciliationSeverity.HIGH,
                "Draw payout total differs from expected outcomes", now));
        }
        if (payoutSummary.paidAmount() != null
            && payoutSummary.paidAmount().amount().compareTo(expectedPayoutTotal) > 0) {
            add(anomalies, counters, anomaly(item, null, null, null, null,
                ReconciliationAnomalyType.DRAW_PAID_TOTAL_EXCEEDS_EXPECTED,
                ReconciliationSeverity.CRITICAL,
                "Paid total exceeds expected draw payout total", now));
        }
        return new DailyReconciliationProcessingResult(counters, List.copyOf(anomalies));
    }

    private void add(
        List<ReconciliationAnomaly> anomalies,
        DailyReconciliationCounters counters,
        ReconciliationAnomaly anomaly
    ) {
        anomalies.add(anomaly);
        counters.add(DailyReconciliationCounters.forSeverity(anomaly.severity()));
    }

    private ReconciliationAnomaly anomaly(
        DailyReconciliationItem item,
        ExpectedTicketOutcomeRow expected,
        ActualTicketStateRow actual,
        PayoutId payoutClaimId,
        PayoutId payoutPaymentId,
        ReconciliationAnomalyType type,
        ReconciliationSeverity severity,
        String message,
        Instant now
    ) {
        var drawResult = item.drawResult();
        var ticketId = ticketId(expected, actual);
        return new ReconciliationAnomaly(
            ReconciliationAnomalyId.of(idGenerator.newUuid()),
            item.tenantId(),
            item.runId(),
            item.businessDate(),
            severity,
            type,
            ReconciliationAnomalyStatus.OPEN,
            ReconciliationAnomalyFingerprintPolicy.fingerprint(type, drawResult.drawId(), ticketId, payoutClaimId == null ? null : payoutClaimId.value(), payoutPaymentId),
            drawResult.drawId(),
            drawResult.drawChannelId(),
            drawResult.drawResultId(),
            ticketId,
            expected != null ? expected.ticketCode() : actual == null ? null : actual.ticketCode(),
            expected != null ? expected.publicCode() : actual == null ? null : actual.publicCode(),
            expected != null ? expected.displayCode() : actual == null ? null : actual.displayCode(),
            payoutClaimId == null ? null : payoutClaimId.value(),
            payoutPaymentId,
            expected == null || expected.expectedResultStatus() == null ? null : expected.expectedResultStatus().name(),
            actual == null || actual.resultStatus() == null ? null : actual.resultStatus().name(),
            expected == null ? null : expected.expectedPayoutAmount(),
            actual == null || actual.actualPotentialPayout() == null ? null : actual.actualPotentialPayout().amount(),
            actual == null || actual.actualPotentialPayout() == null ? null : actual.actualPotentialPayout().currency().value(),
            message,
            "{}",
            now,
            now,
            null
        );
    }

    private TicketId ticketId(ExpectedTicketOutcomeRow expected, ActualTicketStateRow actual) {
        if (expected != null) {
            return expected.ticketId();
        }
        return actual == null ? null : actual.ticketId();
    }

    private boolean isMissingResult(ActualTicketStateRow actual) {
        return actual == null
            || actual.resultStatus() == null
            || actual.resultStatus() == TicketResultStatus.PENDING
            || actual.resultStatus() == TicketResultStatus.NOT_RESULTED;
    }

    private boolean allowsPayment(PayoutClaimForDrawRow claim) {
        return claim.status() == PayoutStatus.APPROVED || claim.status() == PayoutStatus.PAID;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
