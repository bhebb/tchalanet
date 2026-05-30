package com.tchalanet.server.core.terminal.internal.application.command.handler.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.command.CreateTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.command.VerifyTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeCodeGeneratorPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeDeliveryPort;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDelivery;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryMode;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class TerminalActivationChallengeCommandHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final UserId ACTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private static final UUID CHALLENGE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OLD_BINDING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID NEW_BINDING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createsE2eChallengeThroughTestCaptureWithoutStoringClearCode() {
        var terminalStore = new InMemoryTerminalStore(registeredPhysicalTerminal());
        var challengeStore = new InMemoryChallengeStore();
        var delivery = new CapturingDeliveryPort();
        var handler = new CreateTerminalActivationChallengeCommandHandler(
            terminalStore,
            challengeStore,
            type -> "12345678",
            delivery,
            new QueueIdGenerator(CHALLENGE_UUID),
            CLOCK
        );

        var result = handler.handle(new CreateTerminalActivationChallengeCommand(
            TENANT_ID,
            TERMINAL_ID,
            USER_ID,
            TerminalChallengeType.POS_PAIRING,
            TerminalChallengeDeliveryMode.E2E,
            ACTOR_ID
        ));

        assertThat(result.challengeId()).isEqualTo(TerminalActivationChallengeId.of(CHALLENGE_UUID));
        assertThat(result.channel()).isEqualTo(TerminalChallengeChannel.TEST_CAPTURE);
        assertThat(result.deliveryRef()).isEqualTo("test-capture:" + CHALLENGE_UUID);
        assertThat(challengeStore.challenge.codeHash()).doesNotContain("12345678");
        assertThat(delivery.clearCode).isEqualTo("12345678");
    }

    @Test
    void verifiesChallengeConsumesItRevokesPreviousBindingAndCreatesNewBinding() {
        var terminalStore = new InMemoryTerminalStore(registeredPhysicalTerminal());
        var challengeStore = new InMemoryChallengeStore();
        var bindingStore = new InMemoryBindingStore(oldBinding());
        var createHandler = new CreateTerminalActivationChallengeCommandHandler(
            terminalStore,
            challengeStore,
            type -> "12345678",
            new CapturingDeliveryPort(),
            new QueueIdGenerator(CHALLENGE_UUID),
            CLOCK
        );
        createHandler.handle(new CreateTerminalActivationChallengeCommand(
            TENANT_ID,
            TERMINAL_ID,
            USER_ID,
            TerminalChallengeType.POS_PAIRING,
            TerminalChallengeDeliveryMode.E2E,
            ACTOR_ID
        ));
        var verifyHandler = new VerifyTerminalActivationChallengeCommandHandler(
            challengeStore,
            challengeStore,
            terminalStore,
            terminalStore,
            bindingStore,
            bindingStore,
            new QueueIdGenerator(NEW_BINDING_UUID),
            CLOCK
        );

        var result = verifyHandler.handle(new VerifyTerminalActivationChallengeCommand(
            TENANT_ID,
            TerminalActivationChallengeId.of(CHALLENGE_UUID),
            USER_ID,
            "12345678",
            TerminalBindingType.POS_DEVICE,
            "public-key",
            "secret-hash",
            "fingerprint-hash",
            ACTOR_ID
        ));

        assertThat(result.bindingId()).isEqualTo(TerminalBindingId.of(NEW_BINDING_UUID));
        assertThat(challengeStore.challenge.status()).isEqualTo(TerminalChallengeStatus.CONSUMED);
        assertThat(bindingStore.bindings)
            .extracting(TerminalDeviceBinding::status)
            .contains(TerminalBindingStatus.REVOKED, TerminalBindingStatus.ACTIVE);
        assertThat(terminalStore.terminal.state()).isEqualTo(TerminalState.ACTIVE);
    }

    @Test
    void invalidCodeConsumesAnAttemptWithoutCreatingBinding() {
        var terminalStore = new InMemoryTerminalStore(registeredPhysicalTerminal());
        var challengeStore = new InMemoryChallengeStore();
        var bindingStore = new InMemoryBindingStore();
        var createHandler = new CreateTerminalActivationChallengeCommandHandler(
            terminalStore,
            challengeStore,
            fixedCode("12345678"),
            new CapturingDeliveryPort(),
            new QueueIdGenerator(CHALLENGE_UUID),
            CLOCK
        );
        createHandler.handle(new CreateTerminalActivationChallengeCommand(
            TENANT_ID,
            TERMINAL_ID,
            USER_ID,
            TerminalChallengeType.POS_PAIRING,
            TerminalChallengeDeliveryMode.E2E,
            ACTOR_ID
        ));
        var verifyHandler = new VerifyTerminalActivationChallengeCommandHandler(
            challengeStore,
            challengeStore,
            terminalStore,
            terminalStore,
            bindingStore,
            bindingStore,
            new QueueIdGenerator(NEW_BINDING_UUID),
            CLOCK
        );

        assertThatThrownBy(() -> verifyHandler.handle(new VerifyTerminalActivationChallengeCommand(
            TENANT_ID,
            TerminalActivationChallengeId.of(CHALLENGE_UUID),
            USER_ID,
            "00000000",
            TerminalBindingType.POS_DEVICE,
            "public-key",
            "secret-hash",
            "fingerprint-hash",
            ACTOR_ID
        ))).isInstanceOf(ProblemRestException.class);

        assertThat(challengeStore.challenge.attemptCount()).isEqualTo(1);
        assertThat(challengeStore.challenge.status()).isEqualTo(TerminalChallengeStatus.PENDING);
        assertThat(bindingStore.bindings).isEmpty();
    }

    private static TerminalChallengeCodeGeneratorPort fixedCode(String code) {
        return type -> code;
    }

    private static Terminal registeredPhysicalTerminal() {
        return new Terminal(
            TENANT_ID,
            TERMINAL_ID,
            OUTLET_ID,
            USER_ID,
            TerminalKind.PHYSICAL,
            TerminalState.REGISTERED,
            false,
            TerminalSyncState.ONLINE,
            "T-001",
            "Terminal",
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            NOW,
            Map.of("surface", "POS"),
            NOW,
            null
        );
    }

    private static TerminalDeviceBinding oldBinding() {
        return TerminalDeviceBinding.active(
            TerminalBindingId.of(OLD_BINDING_UUID),
            TENANT_ID,
            TERMINAL_ID,
            TerminalBindingType.POS_DEVICE,
            "old-public-key",
            "old-secret-hash",
            "old-fingerprint-hash",
            NOW.minusSeconds(60),
            null
        );
    }

    private static final class QueueIdGenerator implements IdGenerator {
        private final Queue<UUID> ids;

        QueueIdGenerator(UUID... ids) {
            this.ids = new ArrayDeque<>(List.of(ids));
        }

        @Override
        public UUID newUuid() {
            return ids.remove();
        }
    }

    private static final class CapturingDeliveryPort implements TerminalChallengeDeliveryPort {
        private String clearCode;

        @Override
        public TerminalChallengeDelivery deliver(TerminalActivationChallenge challenge, String clearCode) {
            this.clearCode = clearCode;
            return new TerminalChallengeDelivery(
                challenge.id(),
                challenge.challengeType(),
                challenge.channel(),
                "test-capture:" + challenge.id().value(),
                NOW
            );
        }
    }

    private static final class InMemoryChallengeStore
        implements TerminalActivationChallengeReaderPort, TerminalActivationChallengeWriterPort {

        private TerminalActivationChallenge challenge;

        @Override
        public Optional<TerminalActivationChallenge> findById(
            TenantId tenantId,
            TerminalActivationChallengeId challengeId
        ) {
            return Optional.ofNullable(challenge)
                .filter(existing -> existing.tenantId().equals(tenantId))
                .filter(existing -> existing.id().equals(challengeId));
        }

        @Override
        public TerminalActivationChallenge save(TerminalActivationChallenge challenge) {
            this.challenge = challenge;
            return challenge;
        }

        @Override
        public void revokeAllPending(TenantId tenantId, TerminalId terminalId, UserId userId, TerminalChallengeType challengeType) {

        }
    }

    private static final class InMemoryBindingStore
        implements TerminalDeviceBindingReaderPort, TerminalDeviceBindingWriterPort {

        private final List<TerminalDeviceBinding> bindings = new ArrayList<>();

        InMemoryBindingStore(TerminalDeviceBinding... bindings) {
            this.bindings.addAll(List.of(bindings));
        }

        @Override
        public List<TerminalDeviceBinding> findActiveByTerminal(TenantId tenantId, TerminalId terminalId) {
            return bindings.stream()
                .filter(binding -> binding.tenantId().equals(tenantId))
                .filter(binding -> binding.terminalId().equals(terminalId))
                .filter(binding -> binding.status() == TerminalBindingStatus.ACTIVE)
                .toList();
        }

        @Override
        public TerminalDeviceBinding save(TerminalDeviceBinding binding) {
            bindings.removeIf(existing -> existing.id().equals(binding.id()));
            bindings.add(binding);
            return binding;
        }
    }

    private static final class InMemoryTerminalStore implements TerminalReaderPort, TerminalWriterPort {
        private Terminal terminal;

        InMemoryTerminalStore(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId) {
            return Optional.of(terminal)
                .filter(existing -> existing.tenantId().equals(tenantId))
                .filter(existing -> existing.id().equals(terminalId));
        }

        @Override
        public Terminal getById(TenantId tenantId, TerminalId terminalId) {
            return findById(tenantId, terminalId).orElseThrow();
        }

        @Override
        public List<Terminal> listByOutlet(TenantId tenantId, OutletId outletId, PageRequest pageRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Terminal> listByTenant(TenantId tenantId, PageRequest pageRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TchPage<TerminalSummaryView> search(TerminalSearchCriteria criteria, TchPageRequest pageRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TerminalSummaryView> listOffline() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TerminalSummaryView> listSyncPending() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Terminal> findCurrentForUser(UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int countActiveByTenant(TenantId tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Terminal save(Terminal terminal) {
            this.terminal = terminal;
            return terminal;
        }

        @Override
        public void setSalesBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPayoutBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOfflineBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {
            throw new UnsupportedOperationException();
        }
    }
}
