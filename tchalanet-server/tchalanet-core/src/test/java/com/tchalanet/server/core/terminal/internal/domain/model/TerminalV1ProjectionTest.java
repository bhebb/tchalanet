package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalV1ProjectionTest {

    @Test
    void physicalTerminalDefaultsToPosSurface() {
        var terminal = terminal(TerminalKind.PHYSICAL, TerminalState.REGISTERED, Map.of());

        assertThat(terminal.effectiveSurface()).isEqualTo(TerminalSurface.POS);
        assertThat(terminal.lifecycleStatus()).isEqualTo(TerminalStatus.REGISTERED);
    }

    @Test
    void virtualTerminalDefaultsToMobileSurfaceDuringTransition() {
        var terminal = terminal(TerminalKind.VIRTUAL, TerminalState.ACTIVE, Map.of());

        assertThat(terminal.effectiveSurface()).isEqualTo(TerminalSurface.MOBILE);
        assertThat(terminal.lifecycleStatus()).isEqualTo(TerminalStatus.ACTIVE);
    }

    @Test
    void metadataSurfaceOverridesTransitionDefault() {
        var terminal = terminal(TerminalKind.VIRTUAL, TerminalState.ACTIVE, Map.of("surface", "BACK_OFFICE"));

        assertThat(terminal.effectiveSurface()).isEqualTo(TerminalSurface.BACK_OFFICE);
    }

    @Test
    void offlineStateRemainsActiveLifecycleStatus() {
        var terminal = terminal(TerminalKind.PHYSICAL, TerminalState.OFFLINE, Map.of());

        assertThat(terminal.lifecycleStatus()).isEqualTo(TerminalStatus.ACTIVE);
    }

    @Test
    void unregisteredStateMapsToRetiredLifecycleStatus() {
        var terminal = terminal(TerminalKind.PHYSICAL, TerminalState.UNREGISTERED, Map.of());

        assertThat(terminal.lifecycleStatus()).isEqualTo(TerminalStatus.RETIRED);
    }

    private static Terminal terminal(TerminalKind kind, TerminalState state, Map<String, Object> metadata) {
        return new Terminal(
            TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            null,
            kind,
            state,
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
            Instant.parse("2026-05-26T10:00:00Z"),
            metadata,
            Instant.parse("2026-05-26T09:00:00Z"),
            null
        );
    }
}
