package com.tchalanet.server.core.limitpolicy.application.query.model.rules;

import com.tchalanet.server.common.bus.Query;

import java.util.List;

public record ListAvailableLimitRulesQuery()
    implements Query<List<LimitRuleSpec>> {
}
