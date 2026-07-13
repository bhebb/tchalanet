package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPricingRuleOverrideView;
import com.tchalanet.server.core.pricing.api.query.ListSellerTerminalPricingRuleOverridesQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.mapper.SellerTerminalOddsOverrideMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListSellerTerminalPricingRuleOverridesQueryHandler
    implements QueryHandler<ListSellerTerminalPricingRuleOverridesQuery, List<SellerTerminalPricingRuleOverrideView>> {

    private final SellerTerminalOddsOverrideReaderPort reader;
    private final SellerTerminalOddsOverrideMapper mapper;

    @Override
    public List<SellerTerminalPricingRuleOverrideView> handle(ListSellerTerminalPricingRuleOverridesQuery q) {
        return reader.findActiveBySellerTerminal(q.tenantId(), q.sellerTerminalId())
            .stream()
            .map(mapper::toView)
            .toList();
    }
}
