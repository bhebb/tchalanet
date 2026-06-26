package com.tchalanet.server.core.sellerterminal.internal.application.mapper;

import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalForSaleValidationView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;

public final class SellerTerminalViews {

    private SellerTerminalViews() {
    }

    public static SellerTerminalView detail(SellerTerminal t) {
        return new SellerTerminalView(
            t.id(), t.tenantId(),
            t.terminalCode(), t.displayName(), t.firstName(), t.lastName(),
            t.email(), t.phoneNumber(), t.addressId(),
            t.status(), t.commissionRate(),
            t.lastSeenAt(), t.activatedAt(),
            t.blockedAt(), t.blockedBy(), t.blockedReason(), t.disabledAt(),
            t.mustChangePin(), t.pinResetAt());
    }

    public static SellerTerminalSummaryRow summary(SellerTerminal t) {
        return new SellerTerminalSummaryRow(
            t.id(), t.tenantId(),
            t.terminalCode(), t.displayName(), t.email(), t.phoneNumber(),
            t.status(), t.commissionRate(),
            t.lastSeenAt(), t.activatedAt(),
            null, null, null, null);
    }

    public static SellerTerminalForSaleValidationView saleValidation(SellerTerminal t) {
        return new SellerTerminalForSaleValidationView(
            t.id(), t.tenantId(), t.status(), t.commissionRate());
    }
}
