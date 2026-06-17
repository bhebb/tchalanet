package com.tchalanet.server.platform.communication.internal.service.fees;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.platform.communication.api.CommunicationFeePolicy;
import com.tchalanet.server.platform.communication.api.model.request.CommunicationCostBearer;
import com.tchalanet.server.platform.communication.api.model.request.CommunicationFee;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantConfigCommunicationFeePolicy implements CommunicationFeePolicy {

    private final TenantConfigApi tenantConfigApi;

    @Override
    public Optional<CommunicationFee> feeFor(
        TenantId tenantId,
        CommunicationChannel channel
    ) {
        var config = tenantConfigApi.getTenantCommunicationConfig(new GetTenantByIdRequest(tenantId));

        if (config == null || config.buyerTicketDelivery() == null) {
            return Optional.empty();
        }

        var channelConfig = switch (channel) {
            case SMS -> config.buyerTicketDelivery().sms();
            case WHATSAPP -> config.buyerTicketDelivery().whatsapp();
            case EMAIL -> config.buyerTicketDelivery().email();
            default -> null;
        };

        if (channelConfig == null || !Boolean.TRUE.equals(channelConfig.enabled())) {
            return Optional.empty();
        }

        var amount = channelConfig.amount();
        if (amount == null || amount.signum() == 0) {
            return Optional.of(new CommunicationFee(
                channel,
                Money.zero(CurrencyCode.of(channelConfig.currency())),
                CommunicationCostBearer.valueOf(channelConfig.paidBy().trim().toUpperCase())
            ));
        }

        return Optional.of(new CommunicationFee(
            channel,
            new Money(amount, CurrencyCode.of(channelConfig.currency())),
            CommunicationCostBearer.valueOf(channelConfig.paidBy().trim().toUpperCase())
        ));
    }
}
