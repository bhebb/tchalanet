package com.tchalanet.server.core.limitpolicy.application.query.model;

import java.util.UUID;

public record ListPendingValidationsQuery(UUID tenantId, UUID approverId) {

}
