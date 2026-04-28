package com.tchalanet.server.core.terminal.application.port.out;

import com.tchalanet.server.core.terminal.domain.model.Terminal;

public interface TerminalWriterPort {

  Terminal save(Terminal terminal);
}
