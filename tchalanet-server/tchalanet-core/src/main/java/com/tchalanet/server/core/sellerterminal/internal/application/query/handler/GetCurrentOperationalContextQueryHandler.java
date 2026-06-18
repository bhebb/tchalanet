package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.query.CurrentOperationalContextView;
import com.tchalanet.server.core.sellerterminal.api.query.GetCurrentOperationalContextQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentOperationalContextQueryHandler
    implements QueryHandler<GetCurrentOperationalContextQuery, CurrentOperationalContextView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public CurrentOperationalContextView handle(GetCurrentOperationalContextQuery query) {
        if (query.sellerTerminalId() == null) {
            return new CurrentOperationalContextView(
                null,
                null,
                null,
                null,
                OperationalContextSource.NONE,
                OperationalContextTrust.NONE,
                false,
                false);
        }

        var terminal = reader.getRequired(query.tenantId(), query.sellerTerminalId());
        return new CurrentOperationalContextView(
            terminal.id(),
            terminal.terminalCode(),
            terminal.displayName(),
            terminal.status(),
            query.source() != null ? query.source() : OperationalContextSource.NONE,
            query.trust() != null ? query.trust() : OperationalContextTrust.NONE,
            true,
            query.trustedForSensitiveOperation());
    }
}
