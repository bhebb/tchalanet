package com.tchalanet.server.core.limitpolicy.application.query.model.rules;

import java.util.List;

public record LimitRuleCatalog(
    int version,
    List<LimitRuleSpec> rules
) {}

