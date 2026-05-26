package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;

public record TicketPrintCharge(
    TicketChargeType type,
    ChargePaidBy paidBy,
    String label,
    Money amount,
    PromotionRuleId waivedByRuleId
) {
    public boolean isWaived() {
        return waivedByRuleId != null;
    }
}
