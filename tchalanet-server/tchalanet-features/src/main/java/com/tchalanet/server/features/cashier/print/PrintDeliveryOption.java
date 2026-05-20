package com.tchalanet.server.features.cashier.print;

public enum PrintDeliveryOption {
    RETURN_FILE,
    SMS,
    WHATSAPP,
    EMAIL;

    public boolean external() {
        return this == SMS || this == WHATSAPP || this == EMAIL;
    }
}
