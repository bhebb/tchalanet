package com.tchalanet.server.core.sales.internal.application.service.sell;


import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.platform.communication.api.CommunicationFeePolicy;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaleChargeCalculator {

    private final CommunicationFeePolicy communicationFeePolicy;
    // -------------------------------------------------------------------------
    // Charges (notification fees)
    // -------------------------------------------------------------------------

    public List<TicketCharge> compute(TenantId tenantId, SellTicketCommand command) {
        var opts = command.communicationOptions();
        if (opts == null) {
            return List.of();
        }

        var charges = new ArrayList<TicketCharge>();

        if (opts.sendSms()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.SMS,
                TicketChargeType.BUYER_SMS
            );
        }

        if (opts.sendWhatsapp()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.WHATSAPP,
                TicketChargeType.BUYER_WHATSAPP
            );
        }

        if (opts.sendEmail()) {
            appendCommunicationChargeIfApplicable(
                charges,
                tenantId,
                CommunicationChannel.EMAIL,
                TicketChargeType.BUYER_EMAIL
            );
        }

        return List.copyOf(charges);
    }

    private void appendCommunicationChargeIfApplicable(
        List<TicketCharge> out,
        TenantId tenantId,
        CommunicationChannel channel,
        TicketChargeType chargeType
    ) {
        communicationFeePolicy.feeFor(tenantId, channel).ifPresent(fee -> {
            if (fee.amount().isZero()) {
                return;
            }

            out.add(new TicketCharge(
                chargeType,
                fee.amount(),
                ChargePaidBy.fromCommunicationFee(fee.paidBy())
            ));
        });
    }
}
