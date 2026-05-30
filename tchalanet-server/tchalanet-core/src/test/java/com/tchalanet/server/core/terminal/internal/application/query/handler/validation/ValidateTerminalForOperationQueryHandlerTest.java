package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.capability.TerminalCapabilityReaderPort;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSurface;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class ValidateTerminalForOperationQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final OutletId OTHER_OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private static final UserId OTHER_USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000006"));
    private static final TerminalAssignmentId ASSIGNMENT_ID =
        TerminalAssignmentId.of(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    private static final TerminalBindingId BINDING_ID =
        TerminalBindingId.of(UUID.fromString("00000000-0000-0000-0000-000000000008"));
    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void returnsV1SnapshotForValidatedTerminal() {
        var handler = handler(terminal(Map.of("surface", "BACK_OFFICE")));

        var result = handler.handle(query(TerminalOperation.SELL_TICKET));

        assertThat(result.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(result.outletId()).isEqualTo(OUTLET_ID);
        assertThat(result.kind()).isEqualTo(TerminalKind.VIRTUAL);
        assertThat(result.surface()).isEqualTo(TerminalSurface.BACK_OFFICE);
        assertThat(result.status()).isEqualTo(TerminalStatus.ACTIVE);
        assertThat(result.syncState()).isEqualTo(TerminalSyncState.ONLINE);
        assertThat(result.capabilities()).contains(TerminalCapability.SELL_TICKET);
        assertThat(result.bindingStatus()).isEqualTo(TerminalBindingStatus.ACTIVE);
    }

    @Test
    void rejectsOutletMismatch() {
        var handler = handler(terminal(OTHER_OUTLET_ID, USER_ID, false, false, false, null, Map.of()));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsUnassignedActor() {
        var handler = handler(terminal(OUTLET_ID, OTHER_USER_ID, false, false, false, null, Map.of()));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsLockedTerminal() {
        var handler = handler(terminal(OUTLET_ID, USER_ID, false, false, false, NOW, Map.of()));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsSalesBlockedForSellTicket() {
        var handler = handler(terminal(OUTLET_ID, USER_ID, true, false, false, null, Map.of()));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsPayoutBlockedForPayoutClaim() {
        var handler = handler(terminal(OUTLET_ID, USER_ID, false, true, false, null, Map.of()));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.PAYOUT_CLAIM)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsOfflineBlockedForOfflineGrant() {
        var handler = handler(
            terminal(OUTLET_ID, USER_ID, false, false, true, null, Map.of()),
            capabilities(TerminalCapability.OFFLINE_SELL)
        );

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.OFFLINE_GRANT)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsMissingCapability() {
        var handler = handler(terminal(Map.of("surface", "BACK_OFFICE")), Set.of(TerminalCapability.PRINT_TICKET));

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void rejectsMissingCompatibleBinding() {
        var handler = new ValidateTerminalForOperationQueryHandler(
            new StubTerminalReaderPort(terminal(Map.of("surface", "BACK_OFFICE"))),
            new StubAssignmentReaderPort(true),
            new StubBindingReaderPort(List.of()),
            new StubCapabilityReaderPort(capabilities(TerminalCapability.SELL_TICKET)),
            CLOCK
        );

        assertThatThrownBy(() -> handler.handle(query(TerminalOperation.SELL_TICKET)))
            .isInstanceOf(ProblemRestException.class);
    }

    private static ValidateTerminalForOperationQuery query(TerminalOperation operation) {
        return new ValidateTerminalForOperationQuery(TENANT_ID, TERMINAL_ID, OUTLET_ID, USER_ID, operation);
    }

    private static ValidateTerminalForOperationQueryHandler handler(Terminal terminal) {
        return handler(terminal, capabilities(TerminalCapability.SELL_TICKET, TerminalCapability.PAYOUT_CLAIM));
    }

    private static ValidateTerminalForOperationQueryHandler handler(
        Terminal terminal,
        Set<TerminalCapability> capabilities
    ) {
        return new ValidateTerminalForOperationQueryHandler(
            new StubTerminalReaderPort(terminal),
            new StubAssignmentReaderPort(true),
            new StubBindingReaderPort(List.of(bindingFor(terminal))),
            new StubCapabilityReaderPort(capabilities),
            CLOCK
        );
    }

    private static Set<TerminalCapability> capabilities(TerminalCapability... capabilities) {
        return Set.of(capabilities);
    }

    private static TerminalAssignment assignment() {
        return TerminalAssignment.active(ASSIGNMENT_ID, TENANT_ID, TERMINAL_ID, USER_ID, NOW);
    }

    private static TerminalDeviceBinding bindingFor(Terminal terminal) {
        var bindingType = terminal.effectiveSurface() == TerminalSurface.MOBILE
            ? TerminalBindingType.MOBILE_APP
            : TerminalBindingType.ADMIN_SELECTION;
        return TerminalDeviceBinding.active(
            BINDING_ID,
            TENANT_ID,
            TERMINAL_ID,
            bindingType,
            "public-key",
            "secret-hash",
            "fingerprint-hash",
            NOW.minusSeconds(60),
            NOW.plusSeconds(600)
        );
    }

    private static Terminal terminal(Map<String, Object> metadata) {
        return terminal(OUTLET_ID, USER_ID, false, false, false, null, metadata);
    }

    private static Terminal terminal(
        OutletId outletId,
        UserId assignedUserId,
        boolean salesBlocked,
        boolean payoutBlocked,
        boolean offlineBlocked,
        Instant lockedAt,
        Map<String, Object> metadata
    ) {
        return new Terminal(
            TENANT_ID,
            TERMINAL_ID,
            outletId,
            assignedUserId,
            TerminalKind.VIRTUAL,
            TerminalState.ACTIVE,
            false,
            TerminalSyncState.ONLINE,
            "T-001",
            "Terminal",
            null,
            lockedAt,
            lockedAt == null ? null : USER_ID,
            lockedAt == null ? null : "locked",
            salesBlocked,
            salesBlocked ? "sales blocked" : null,
            salesBlocked ? NOW : null,
            salesBlocked ? USER_ID : null,
            payoutBlocked,
            payoutBlocked ? "payout blocked" : null,
            payoutBlocked ? NOW : null,
            payoutBlocked ? USER_ID : null,
            offlineBlocked,
            offlineBlocked ? "offline blocked" : null,
            offlineBlocked ? NOW : null,
            offlineBlocked ? USER_ID : null,
            NOW,
            metadata,
            NOW,
            null
        );
    }

    private record StubTerminalReaderPort(Terminal terminal) implements TerminalReaderPort {
        @Override
        public Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId) {
            return Optional.of(terminal);
        }

        @Override
        public Terminal getById(TenantId tenantId, TerminalId terminalId) {
            return terminal;
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
    }

    private record StubAssignmentReaderPort(boolean exists) implements TerminalAssignmentReaderPort {
        @Override
        public Optional<TerminalAssignment> findActive(TenantId tenantId, TerminalId terminalId, UserId userId) {
            return exists && userId.equals(USER_ID) ? Optional.of(assignment()) : Optional.empty();
        }

        @Override
        public Optional<TerminalAssignment> findActiveAssignmentByTerminal(TenantId tenantId, TerminalId terminalId) {
            return exists ? Optional.of(assignment()) : Optional.empty();
        }
    }

    private record StubBindingReaderPort(List<TerminalDeviceBinding> bindings) implements TerminalDeviceBindingReaderPort {
        @Override
        public List<TerminalDeviceBinding> findActiveByTerminal(TenantId tenantId, TerminalId terminalId) {
            return bindings;
        }
    }

    private record StubCapabilityReaderPort(Set<TerminalCapability> capabilities) implements TerminalCapabilityReaderPort {
        @Override
        public Set<TerminalCapability> findByTerminal(TenantId tenantId, TerminalId terminalId) {
            return capabilities;
        }
    }
}
