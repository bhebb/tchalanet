package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

/**
 * Heartbeat sent by an online terminal. No event is published per heartbeat (too noisy); the sync
 * state is updated to ONLINE if it was OFFLINE.
 */
public record SendTerminalHeartbeatCommand(
    TenantId tenantId, TerminalId terminalId, Instant occurredAt) implements Command<Void> {}
