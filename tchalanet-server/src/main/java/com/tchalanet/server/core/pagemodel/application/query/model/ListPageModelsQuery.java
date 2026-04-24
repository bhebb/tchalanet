package com.tchalanet.server.core.pagemodel.application.query.model;

import com.tchalanet.server.common.bus.Query;

import java.util.UUID;

public record ListPageModelsQuery(UUID tenantId, String scope, String logicalId)
    implements Query<Object> {}
