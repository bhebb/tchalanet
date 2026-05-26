package com.tchalanet.server.core.terminal.internal.application.port.out.assignment;

import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;

public interface TerminalAssignmentWriterPort {

    TerminalAssignment save(TerminalAssignment assignment);
}
