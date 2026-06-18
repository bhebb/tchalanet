package com.tchalanet.server.core.sellerterminal.internal.application.port.out;

import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;

public interface SellerTerminalWriterPort {

    SellerTerminal save(SellerTerminal terminal);
}
