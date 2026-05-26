package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.core.terminal.internal.domain.model.lifecycle.TerminalLifecyclePolicy;
import org.junit.jupiter.api.Test;

class TerminalLifecyclePolicyTest {

    @Test
    void registeredCanMoveToPendingActivationRevokedOrRetired() {
        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.REGISTERED,
            TerminalStatus.PENDING_ACTIVATION
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.REGISTERED,
            TerminalStatus.REVOKED
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.REGISTERED,
            TerminalStatus.RETIRED
        )).isTrue();
    }

    @Test
    void pendingActivationCanBecomeActiveRevokedOrRetired() {
        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.PENDING_ACTIVATION,
            TerminalStatus.ACTIVE
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.PENDING_ACTIVATION,
            TerminalStatus.REVOKED
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.PENDING_ACTIVATION,
            TerminalStatus.RETIRED
        )).isTrue();
    }

    @Test
    void activeTerminalCanBeLockedRevokedOrRetired() {
        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.ACTIVE,
            TerminalStatus.LOCKED
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.ACTIVE,
            TerminalStatus.REVOKED
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.ACTIVE,
            TerminalStatus.RETIRED
        )).isTrue();
    }

    @Test
    void lockedTerminalCanOnlyBeUnlockedRevokedOrRetired() {
        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.LOCKED,
            TerminalStatus.ACTIVE
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.LOCKED,
            TerminalStatus.REVOKED
        )).isTrue();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.LOCKED,
            TerminalStatus.PENDING_ACTIVATION
        )).isFalse();
    }

    @Test
    void revokedAndRetiredAreTerminalStates() {
        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.REVOKED,
            TerminalStatus.ACTIVE
        )).isFalse();

        assertThat(TerminalLifecyclePolicy.canTransition(
            TerminalStatus.RETIRED,
            TerminalStatus.ACTIVE
        )).isFalse();
    }

    @Test
    void requireTransitionFailsForForbiddenTransition() {
        assertThatThrownBy(() -> TerminalLifecyclePolicy.requireTransition(
            TerminalStatus.REVOKED,
            TerminalStatus.ACTIVE
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("terminal.status_transition_forbidden");
    }
}
