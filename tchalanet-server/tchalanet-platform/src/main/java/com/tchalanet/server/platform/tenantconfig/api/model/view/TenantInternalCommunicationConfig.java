package com.tchalanet.server.platform.tenantconfig.api.model.view;

import java.math.BigDecimal;

public record TenantInternalCommunicationConfig(BuyerTicketDeliveryConfig buyerTicketDelivery) {

    public record BuyerTicketDeliveryConfig(
        DeliveryChannelConfig sms,
        DeliveryChannelConfig whatsapp,
        DeliveryChannelConfig email) {}

    public record DeliveryChannelConfig(
        boolean enabled,
        BigDecimal amount,
        String currency,
        String paidBy) {}
}

