package com.tchalanet.server.core.pos.application.port.out;

import com.tchalanet.server.core.pos.domain.model.Terminal;

public interface TerminalWriterPort {

  Terminal save(Terminal terminal);
}
