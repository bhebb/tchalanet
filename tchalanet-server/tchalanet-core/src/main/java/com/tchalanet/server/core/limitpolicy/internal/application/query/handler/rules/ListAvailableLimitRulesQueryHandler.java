package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.rules;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.query.model.rules.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.application.query.model.rules.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.application.service.LimitRuleCatalogLoader;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListAvailableLimitRulesQueryHandler
    implements QueryHandler<ListAvailableLimitRulesQuery, List<LimitRuleSpec>> {

    private final LimitRuleCatalogLoader loader;

    @Override
    public List<LimitRuleSpec> handle(ListAvailableLimitRulesQuery query) {
        return loader.listAvailableRules();
    }
}
