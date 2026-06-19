package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalForSaleValidationView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalForSaleValidationQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.mapper.SellerTerminalViews;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalForSaleValidationQueryHandler
    implements QueryHandler<GetSellerTerminalForSaleValidationQuery, SellerTerminalForSaleValidationView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalForSaleValidationView handle(GetSellerTerminalForSaleValidationQuery q) {
        return SellerTerminalViews.saleValidation(reader.getRequired(q.tenantId(), q.terminalId()));
    }
}
