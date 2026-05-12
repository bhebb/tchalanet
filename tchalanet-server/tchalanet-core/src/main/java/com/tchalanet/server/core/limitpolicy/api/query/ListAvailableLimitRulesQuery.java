package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;

import java.util.List;

public record ListAvailableLimitRulesQuery()
    implements Query<List<LimitRuleSpec>> {
}
