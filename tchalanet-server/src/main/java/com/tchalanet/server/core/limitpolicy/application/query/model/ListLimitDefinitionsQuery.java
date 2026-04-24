package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.bus.Query;

/** Empty query to request all limit definitions visible to the current tenant (RLS applies). */
public record ListLimitDefinitionsQuery() implements Query<ListLimitDefinitionsView> {}
