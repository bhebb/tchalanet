package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import jakarta.validation.constraints.NotNull;

public record CashierSendReceiptRequest(
    @NotNull CommunicationChannel channel,
    String to,
    String channelKey,
    Boolean includeVerificationLink
) {}
