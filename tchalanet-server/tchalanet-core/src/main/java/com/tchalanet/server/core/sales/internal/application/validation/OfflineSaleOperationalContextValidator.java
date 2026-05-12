package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.application.command.model.OfflineTicketSaleInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflineSaleOperationalContextValidator {

    private final QueryBus queryBus;

    public ValidatedOfflineSaleOperationalContext validateForOfflineSync(
        OfflineTicketSaleInput input) {

        var terminal =
            queryBus.ask(
                new ValidateTerminalForOfflineSyncQuery(
                    input.tenantId(),
                    input.terminalId(),
                    input.outletId(),
                    input.sellerUserId()));

        var outlet =
            queryBus.ask(
                new ValidateOutletForOfflineSyncQuery(
                    input.tenantId(),
                    input.outletId()));

        var session =
            queryBus.ask(
                new GetSalesSessionForOfflineSyncQuery(
                    input.tenantId(),
                    input.salesSessionId(),
                    input.terminalId(),
                    input.outletId(),
                    input.sellerUserId()));

        return new ValidatedOfflineSaleOperationalContext(
            terminal.terminalId(),
            outlet.outletId(),
            session.sessionId(),
            session.status(),
            session.closedAt(),
            session.finalized());
    }
}
