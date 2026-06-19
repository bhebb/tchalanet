package com.tchalanet.server.core.sellerterminal.internal.domain.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;

public class SellerTerminalStatusException extends RuntimeException {

    public SellerTerminalStatusException(SellerTerminalId id, SellerTerminalStatus current, String message) {
        super(message + " [id=" + id.value() + ", status=" + current + "]");
    }
}
