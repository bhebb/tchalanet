package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import jakarta.validation.constraints.NotBlank;

public record GetTicketForCashierVerificationQuery(
    @NotBlank String publicCode
) implements Query<TicketCashierVerificationView> {
}
