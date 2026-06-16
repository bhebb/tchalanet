package com.tchalanet.server.core.terminal.internal.application.query.handler.sellerterminal;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalForSaleValidationView;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalForSaleValidationQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalForSaleValidationQueryHandler
    implements QueryHandler<GetSellerTerminalForSaleValidationQuery, SellerTerminalForSaleValidationView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalForSaleValidationView handle(GetSellerTerminalForSaleValidationQuery q) {
        return SellerTerminalForSaleValidationView.from(
            reader.getRequired(q.tenantId(), q.terminalId()));
    }
}
