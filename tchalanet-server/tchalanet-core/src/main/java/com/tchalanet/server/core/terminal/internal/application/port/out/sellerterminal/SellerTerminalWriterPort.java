package com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal;

import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;

public interface SellerTerminalWriterPort {

    SellerTerminal save(SellerTerminal terminal);
}
