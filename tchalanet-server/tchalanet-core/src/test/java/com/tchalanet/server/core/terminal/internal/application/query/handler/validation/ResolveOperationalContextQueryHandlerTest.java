package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.query.ResolveOperationalContextQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.service.binding.TerminalBindingCredentialHasher;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class ResolveOperationalContextQueryHandlerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID = OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final TerminalAssignmentId ASSIGNMENT_ID =
        TerminalAssignmentId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private static final TerminalBindingId BINDING_ID =
        TerminalBindingId.of(UUID.fromString("00000000-0000-0000-0000-000000000006"));
    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String CREDENTIAL = "binding-token";

    @Test
    void defaultsOperationalFrameToWeakClientClaimWithoutBindingCredential() {
        var result = handler(List.of(binding(CREDENTIAL)), true)
            .handle(query(null));

        assertThat(result.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(result.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    @Test
    void promotesToSignedDeviceBindingWhenCredentialMatchesActiveAssignmentAndBinding() {
        var result = handler(List.of(binding(CREDENTIAL)), true)
            .handle(query(CREDENTIAL));

        assertThat(result.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(result.outletId()).isEqualTo(OUTLET_ID);
        assertThat(result.source()).isEqualTo(OperationalContextSource.SIGNED_DEVICE_BINDING);
        assertThat(result.trust()).isEqualTo(OperationalContextTrust.STRONG);
    }

    @Test
    void keepsWeakClientClaimWhenCredentialDoesNotMatchBindingHash() {
        var result = handler(List.of(binding("other-token")), true)
            .handle(query(CREDENTIAL));

        assertThat(result.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(result.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    @Test
    void keepsWeakClientClaimWhenTerminalIsNotAssignedToActor() {
        var result = handler(List.of(binding(CREDENTIAL)), false)
            .handle(query(CREDENTIAL));

        assertThat(result.source()).isEqualTo(OperationalContextSource.CLIENT_CLAIM);
        assertThat(result.trust()).isEqualTo(OperationalContextTrust.WEAK);
    }

    private static ResolveOperationalContextQuery query(String credential) {
        return new ResolveOperationalContextQuery(
            TENANT_ID,
            USER_ID,
            new OperationalContextHint(
                TERMINAL_ID,
                OUTLET_ID,
                null,
                OperationalContextSource.CLIENT_CLAIM,
                OperationalContextTrust.WEAK),
            credential);
    }

    private static ResolveOperationalContextQueryHandler handler(
        List<TerminalDeviceBinding> bindings,
        boolean assignmentExists
    ) {
        return new ResolveOperationalContextQueryHandler(
            new StubTerminalReaderPort(terminal()),
            new StubAssignmentReaderPort(assignmentExists),
            new StubBindingReaderPort(bindings),
            CLOCK
        );
    }

    private static TerminalDeviceBinding binding(String credential) {
        return TerminalDeviceBinding.active(
            BINDING_ID,
            TENANT_ID,
            TERMINAL_ID,
            TerminalBindingType.POS_DEVICE,
            null,
            null,
            null,
            TerminalBindingCredentialHasher.hash(TENANT_ID, TERMINAL_ID, credential),
            "fingerprint-hash",
            USER_ID,
            NOW.minusSeconds(60),
            NOW.plusSeconds(600)
        );
    }

    private static Terminal terminal() {
        return new Terminal(
            TENANT_ID,
            TERMINAL_ID,
            OUTLET_ID,
            USER_ID,
            TerminalKind.PHYSICAL,
            TerminalState.ACTIVE,
            false,
            TerminalSyncState.ONLINE,
            "POS-001",
            "POS 001",
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

    private static TerminalAssignment assignment() {
        return TerminalAssignment.active(ASSIGNMENT_ID, TENANT_ID, TERMINAL_ID, USER_ID, NOW);
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
            return exists ? Optional.of(assignment()) : Optional.empty();
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

        @Override
        public java.util.Optional<TerminalDeviceBinding> findById(TenantId tenantId, com.tchalanet.server.common.types.id.TerminalBindingId bindingId) {
            return bindings.stream().filter(b -> b.id().equals(bindingId)).findFirst();
        }
    }
}
