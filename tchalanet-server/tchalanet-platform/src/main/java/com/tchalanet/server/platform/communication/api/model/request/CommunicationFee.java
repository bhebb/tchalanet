package com.tchalanet.server.platform.communication.api.model.request;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;

import java.util.Objects;

/**
 * Fee configuration for a tenant/channel pair.
 */
public record CommunicationFee(
    CommunicationChannel channel,
    Money amount,
    CommunicationCostBearer paidBy
) {
    public CommunicationFee {
        Objects.requireNonNull(channel, "channel is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(paidBy, "paidBy is required");
    }
}
