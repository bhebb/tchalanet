package com.tchalanet.server.core.offlinesync.internal.infra.event;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.service.OfflineSyncPromotionPolicy;
import com.tchalanet.server.core.sales.api.event.OfflineSubmissionProcessedEvent;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;

/**
 * Applies the outcome of an {@link OfflineSubmissionProcessedEvent} (published by
 * {@code core.sales}) back onto the offline submission aggregate — provided the event
 * matches the current {@code promotionAttemptId}. Stale results are ignored.
 *
 * <p>Idempotent via {@link ProcessedEventPort}; runs in its own transaction after the
 * {@code sales} commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineSubmissionProcessedEventListener {

    static final String HANDLER_KEY = "offlinesync.sales-offline-promotion-return";

    private final OfflineSubmissionReaderPort submissionReader;
    private final OfflineSubmissionWriterPort submissionWriter;
    private final ProcessedEventPort processedEventPort;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProcessed(OfflineSubmissionProcessedEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(HANDLER_KEY, event.eventId().value())) {
            log.debug("offlinesync: processed event {} already applied, skipping", event.eventId());
            return;
        }

        var submission = submissionReader.findById(event.submissionId()).orElse(null);
        if (submission == null) {
            log.warn("offlinesync: processed event {} references unknown submission {}",
                event.eventId(), event.submissionId());
            return;
        }

        var outcome = OfflineSyncPromotionPolicy.evaluateReturn(submission, event.promotionAttemptId());
        if (outcome instanceof OfflineSyncPromotionPolicy.Outcome.Ignore ignore) {
            log.info("offlinesync: ignoring stale promotion event {} for submission {} — {}",
                event.eventId(), event.submissionId(), ignore.reason());
            return;
        }

        var now = clock.instant();
        var updated = switch (event.outcome()) {
            case PROMOTED -> submission.markPromoted(event.ticketId(), event.eventId(), now);
            case BUSINESS_REJECTED -> submission.markBusinessRejected(
                event.rejectionCode(), event.rejectionReason(), event.eventId(), now);
            case DUPLICATE -> {
                log.info("offlinesync: sales reported DUPLICATE for submission {} — no state change",
                    event.submissionId());
                yield submission;
            }
        };
        submissionWriter.save(updated);
    }
}
