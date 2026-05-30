package com.tchalanet.server.core.terminal.internal.application.port.out.assignment;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import java.util.Optional;

public interface TerminalAssignmentReaderPort {

    Optional<TerminalAssignment> findActive(TenantId tenantId, TerminalId terminalId, UserId userId);

    /**
     * Current active assignment on a terminal, regardless of the user. Used when reassigning a
     * terminal so the previous holder's active assignment can be revoked, preserving the
     * "one active assignment per terminal" invariant.
     */
    Optional<TerminalAssignment> findActiveAssignmentByTerminal(TenantId tenantId, TerminalId terminalId);
}
