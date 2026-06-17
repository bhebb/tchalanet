package com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalStatus;

public class SellerTerminalStatusException extends RuntimeException {

    public SellerTerminalStatusException(SellerTerminalId id, SellerTerminalStatus current, String message) {
        super(message + " [id=" + id.value() + ", status=" + current + "]");
    }
}
