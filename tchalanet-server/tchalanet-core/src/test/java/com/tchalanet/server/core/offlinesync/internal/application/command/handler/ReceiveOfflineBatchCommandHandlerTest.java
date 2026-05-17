package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSalesGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
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

@DisplayName("ReceiveOfflineBatchCommandHandler")
class ReceiveOfflineBatchCommandHandlerTest {

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());
    private final OfflineSalesGrantId grantId = OfflineSalesGrantId.of(UUID.randomUUID());

    private OfflineSalesGrant grant;
    private final Map<String, OfflineSubmission> byClientSaleId = new HashMap<>();
    private final List<OfflineSubmission> saved = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    private final OfflineSalesGrantReaderPort grantReader = new OfflineSalesGrantReaderPort() {
        @Override public OfflineSalesGrant getById(OfflineSalesGrantId id) { return grant; }
        @Override public Optional<OfflineSalesGrant> findById(OfflineSalesGrantId id) { return Optional.of(grant); }
    };

    private final OfflineSubmissionReaderPort submissionReader = new OfflineSubmissionReaderPort() {
        @Override public OfflineSubmission getById(com.tchalanet.server.common.types.id.OfflineSaleSubmissionId id) { return null; }
        @Override public Optional<OfflineSubmission> findByGrantAndClientSaleId(OfflineSalesGrantId g, String clientSaleId) {
            return Optional.ofNullable(byClientSaleId.get(clientSaleId));
        }
        @Override public List<OfflineSubmission> findReadyForDispatch(TenantId t, int limit, Instant now) { return List.of(); }
    };

    private final OfflineSubmissionWriterPort writer = new OfflineSubmissionWriterPort() {
        @Override public OfflineSubmission save(OfflineSubmission s) { saved.add(s); byClientSaleId.put(s.clientSaleId(), s); return s; }
        @Override public List<OfflineSubmission> saveAll(List<OfflineSubmission> ss) { saved.addAll(ss); ss.forEach(s -> byClientSaleId.put(s.clientSaleId(), s)); return ss; }
        @Override public int claimForProcessing(List<OfflineSubmission> ss) { return ss.size(); }
    };

    private final DomainEventPublisher publisher = new DomainEventPublisher() {
        @Override public void publish(DomainEvent e) { events.add(e); }
        @Override public void publish(Collection<? extends DomainEvent> es) { events.addAll(es); }
    };

    private ReceiveOfflineBatchCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReceiveOfflineBatchCommandHandler(
            grantReader, submissionReader, writer, publisher, () -> UUID.randomUUID(), Clock.systemUTC());

        grant = new OfflineSalesGrant(
            grantId, tenantId,
            TerminalId.of(UUID.randomUUID()), OutletId.of(UUID.randomUUID()),
            UserId.of(UUID.randomUUID()),
            OfflineSalesGrantStatus.ACTIVE,
            Instant.now(), Instant.now().plusSeconds(3600),
            null, null);
    }

    @Nested
    @DisplayName("when grant is active")
    class WhenGrantActive {

        @Test
        @DisplayName("persists all submissions as READY_FOR_SALES")
        void persistsSubmissions() {
            var command = new ReceiveOfflineBatchCommand(tenantId, grantId, "sig",
                List.of(
                    new ReceiveOfflineBatchCommand.OfflineSaleSubmissionPayload("sale-001", "{}"),
                    new ReceiveOfflineBatchCommand.OfflineSaleSubmissionPayload("sale-002", "{}")));

            handler.handle(command);

            assertThat(saved).hasSize(2);
        }

        @Test
        @DisplayName("returns a batch id")
        void returnsBatchId() {
            var command = new ReceiveOfflineBatchCommand(tenantId, grantId, "sig",
                List.of(new ReceiveOfflineBatchCommand.OfflineSaleSubmissionPayload("sale-001", "{}")));

            var batchId = handler.handle(command);

            assertThat(batchId).isNotNull();
        }

        @Test
        @DisplayName("skips duplicate client_sale_id — idempotent receipt")
        void skipsDuplicateClientSaleId() {
            var payload = new ReceiveOfflineBatchCommand.OfflineSaleSubmissionPayload("sale-001", "{}");
            var command = new ReceiveOfflineBatchCommand(tenantId, grantId, "sig", List.of(payload));

            handler.handle(command);
            saved.clear();
            events.clear();

            handler.handle(command);

            assertThat(saved).isEmpty();
        }
    }

    @Nested
    @DisplayName("when grant is expired")
    class WhenGrantExpired {

        @BeforeEach
        void setUp() {
            grant = new OfflineSalesGrant(
                grantId, tenantId,
                TerminalId.of(UUID.randomUUID()), OutletId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                OfflineSalesGrantStatus.EXPIRED,
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600),
                null, null);
        }

        @Test
        @DisplayName("throws IllegalStateException")
        void throwsException() {
            var command = new ReceiveOfflineBatchCommand(tenantId, grantId, "sig",
                List.of(new ReceiveOfflineBatchCommand.OfflineSaleSubmissionPayload("sale-001", "{}")));

            assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
