package com.tchalanet.server.core.terminal.internal.infra.web.admin.model;

import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import jakarta.validation.constraints.NotNull;

public record UpdateSyncStateRequest(@NotNull TerminalSyncState newSyncState) {
}
