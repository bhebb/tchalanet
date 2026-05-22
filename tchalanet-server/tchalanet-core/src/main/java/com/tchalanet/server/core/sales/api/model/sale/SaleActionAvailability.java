package com.tchalanet.server.core.sales.api.model.sale;

public record SaleActionAvailability(
    boolean canSell,
    boolean canPrint,
    boolean canSendSms,
    boolean canSendWhatsapp,
    boolean canSendEmail,
    boolean canCopy
) {
    public static SaleActionAvailability rejected() {
        return new SaleActionAvailability(false, false, false, false, false, false);
    }

    public static SaleActionAvailability accepted() {
        return new SaleActionAvailability(false, true, true, true, true, true);
    }
}
