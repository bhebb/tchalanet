package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.RecordOfflineSubmissionSalesDecisionCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmission;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RecordOfflineSubmissionSalesDecisionCommandHandler")
class RecordOfflineSubmissionSalesDecisionCommandHandlerTest {

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());
    private final OfflineSaleSubmissionId submissionId = OfflineSaleSubmissionId.of(UUID.randomUUID());
    private final TicketId ticketId = TicketId.of(UUID.randomUUID());

    private final Map<OfflineSaleSubmissionId, OfflineSubmission> store = new HashMap<>();
    private final List<OfflineSubmission> saved = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    private final OfflineSubmissionReaderPort reader = new OfflineSubmissionReaderPort() {
        @Override public OfflineSubmission getById(OfflineSaleSubmissionId id) { return store.get(id); }
        @Override public Optional<OfflineSubmission> findByGrantAndClientSaleId(OfflineSalesGrantId grantId, String clientSaleId) { return Optional.empty(); }
        @Override public List<OfflineSubmission> findReadyForDispatch(TenantId t, int limit, Instant now) { return List.of(); }
    };

    private final OfflineSubmissionWriterPort writer = new OfflineSubmissionWriterPort() {
        @Override public OfflineSubmission save(OfflineSubmission s) { saved.add(s); store.put(s.id(), s); return s; }
        @Override public List<OfflineSubmission> saveAll(List<OfflineSubmission> ss) { saved.addAll(ss); ss.forEach(s -> store.put(s.id(), s)); return ss; }
        @Override public int claimForProcessing(List<OfflineSubmission> ss) { return ss.size(); }
    };

    private final DomainEventPublisher publisher = new DomainEventPublisher() {
        @Override public void publish(DomainEvent e) { events.add(e); }
        @Override public void publish(Collection<? extends DomainEvent> es) { events.addAll(es); }
    };

    private RecordOfflineSubmissionSalesDecisionCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RecordOfflineSubmissionSalesDecisionCommandHandler(
            reader, writer, publisher, () -> UUID.randomUUID(), Clock.systemUTC());

        var submission = new OfflineSubmission(
            submissionId, tenantId, OfflineBatchId.of(UUID.randomUUID()),
            OfflineSalesGrantId.of(UUID.randomUUID()), "client-001", "{}",
            OfflineSubmissionStatus.SALES_PROCESSING, null, null, null, 1, null, Instant.now(), null);
        store.put(submissionId, submission);
    }

    @Nested
    @DisplayName("recording SALES_ACCEPTED")
    class RecordingAccepted {

        @Test
        @DisplayName("updates status to SALES_ACCEPTED with ticket id")
        void updatesStatus() {
            handler.handle(new RecordOfflineSubmissionSalesDecisionCommand(
                tenantId, submissionId, OfflineSubmissionStatus.SALES_ACCEPTED, ticketId, null));

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).status()).isEqualTo(OfflineSubmissionStatus.SALES_ACCEPTED);
            assertThat(saved.get(0).ticketId()).isEqualTo(ticketId);
        }

        @Test
        @DisplayName("is idempotent — second call with already ACCEPTED submission is a no-op")
        void isIdempotent() {
            handler.handle(new RecordOfflineSubmissionSalesDecisionCommand(
                tenantId, submissionId, OfflineSubmissionStatus.SALES_ACCEPTED, ticketId, null));

            saved.clear();
            events.clear();

            handler.handle(new RecordOfflineSubmissionSalesDecisionCommand(
                tenantId, submissionId, OfflineSubmissionStatus.SALES_ACCEPTED, ticketId, null));

            assertThat(saved).isEmpty();
        }
    }

    @Nested
    @DisplayName("recording SALES_REJECTED")
    class RecordingRejected {

        @Test
        @DisplayName("updates status to SALES_REJECTED with rejection value")
        void updatesStatus() {
            handler.handle(new RecordOfflineSubmissionSalesDecisionCommand(
                tenantId, submissionId, OfflineSubmissionStatus.SALES_REJECTED, null, "DRAW_CUTOFF_EXCEEDED"));

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).status()).isEqualTo(OfflineSubmissionStatus.SALES_REJECTED);
            assertThat(saved.get(0).salesRejectionCode()).isEqualTo("DRAW_CUTOFF_EXCEEDED");
        }
    }
}
