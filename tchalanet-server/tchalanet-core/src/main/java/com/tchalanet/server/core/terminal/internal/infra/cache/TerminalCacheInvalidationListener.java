package com.tchalanet.server.core.terminal.internal.infra.cache;

import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAssignedToOutletEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAssignedToUserEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAutoSessionDisabledEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAutoSessionEnabledEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalLockedEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalMetadataUpdatedEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalRegisteredEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalSyncStateUpdatedEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalUnlockedEvent;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalUnregisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalCacheInvalidationListener {

    private final TerminalCacheEvictor evictor;

    @EventListener
    public void onTerminalRegistered(TerminalRegisteredEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalUnregistered(TerminalUnregisteredEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalLocked(TerminalLockedEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalUnlocked(TerminalUnlockedEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalAssignedToOutlet(TerminalAssignedToOutletEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalAssignedToUser(TerminalAssignedToUserEvent event) {
        evictor.evictTerminalAndUser(event.terminalId(), event.userId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalAutoSessionEnabled(TerminalAutoSessionEnabledEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictCurrentForUser(event.userId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalAutoSessionDisabled(TerminalAutoSessionDisabledEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }

    @EventListener
    public void onTerminalMetadataUpdated(TerminalMetadataUpdatedEvent event) {
        evictor.evictTerminal(event.terminalId());
    }

    @EventListener
    public void onTerminalSyncStateUpdated(TerminalSyncStateUpdatedEvent event) {
        evictor.evictTerminal(event.terminalId());
        evictor.evictTerminalLists();
    }
}
