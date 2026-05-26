package com.tchalanet.server.core.terminal.internal.domain.model.lifecycle;

import com.tchalanet.server.core.terminal.internal.domain.model.TerminalStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TerminalLifecyclePolicy {

    private static final Map<TerminalStatus, Set<TerminalStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TerminalStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
            TerminalStatus.REGISTERED,
            EnumSet.of(TerminalStatus.PENDING_ACTIVATION, TerminalStatus.REVOKED, TerminalStatus.RETIRED)
        );
        ALLOWED_TRANSITIONS.put(
            TerminalStatus.PENDING_ACTIVATION,
            EnumSet.of(TerminalStatus.ACTIVE, TerminalStatus.REVOKED, TerminalStatus.RETIRED)
        );
        ALLOWED_TRANSITIONS.put(
            TerminalStatus.ACTIVE,
            EnumSet.of(TerminalStatus.LOCKED, TerminalStatus.REVOKED, TerminalStatus.RETIRED)
        );
        ALLOWED_TRANSITIONS.put(
            TerminalStatus.LOCKED,
            EnumSet.of(TerminalStatus.ACTIVE, TerminalStatus.REVOKED, TerminalStatus.RETIRED)
        );
        ALLOWED_TRANSITIONS.put(TerminalStatus.REVOKED, EnumSet.noneOf(TerminalStatus.class));
        ALLOWED_TRANSITIONS.put(TerminalStatus.RETIRED, EnumSet.noneOf(TerminalStatus.class));
    }

    private TerminalLifecyclePolicy() {
    }

    public static boolean canTransition(TerminalStatus from, TerminalStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(TerminalStatus from, TerminalStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("terminal.status_transition_forbidden");
        }
    }
}
