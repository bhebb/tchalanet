package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsOverrideView;
import com.tchalanet.server.core.pricing.api.query.ListSellerTerminalOddsOverridesQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.mapper.SellerTerminalOddsOverrideMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListSellerTerminalOddsOverridesQueryHandler
    implements QueryHandler<ListSellerTerminalOddsOverridesQuery, List<SellerTerminalOddsOverrideView>> {

    private final SellerTerminalOddsOverrideReaderPort reader;
    private final SellerTerminalOddsOverrideMapper mapper;

    @Override
    public List<SellerTerminalOddsOverrideView> handle(ListSellerTerminalOddsOverridesQuery q) {
        return reader.findActiveBySellerTerminal(q.tenantId(), q.sellerTerminalId())
            .stream()
            .map(mapper::toView)
            .toList();
    }
}
