package com.tchalanet.server.core.limitpolicy.api.query;

import java.util.List;

public record LimitRuleCatalog(
    int version,
    List<LimitRuleSpec> rules
) {}

