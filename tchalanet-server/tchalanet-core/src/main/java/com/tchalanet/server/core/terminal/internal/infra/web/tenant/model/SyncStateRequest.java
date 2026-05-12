package com.tchalanet.server.core.terminal.internal.infra.web.tenant.model;

import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import jakarta.validation.constraints.NotNull;

public record SyncStateRequest(@NotNull TerminalSyncState newSyncState) {}
