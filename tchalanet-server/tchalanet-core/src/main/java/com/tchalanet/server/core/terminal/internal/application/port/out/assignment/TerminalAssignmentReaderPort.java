package com.tchalanet.server.core.terminal.internal.application.port.out.assignment;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import java.util.Optional;

public interface TerminalAssignmentReaderPort {

    Optional<TerminalAssignment> findActive(TenantId tenantId, TerminalId terminalId, UserId userId);
}
