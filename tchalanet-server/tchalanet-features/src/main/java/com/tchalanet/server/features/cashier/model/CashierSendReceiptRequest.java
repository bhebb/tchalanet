package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.common.communication.api.CommunicationChannel;
import jakarta.validation.constraints.NotNull;

public record CashierSendReceiptRequest(
    @NotNull CommunicationChannel channel,
    String to,
    String channelKey,
    Boolean includeVerificationLink
) {}
