package com.tchalanet.server.core.draw.application.query.model;

import java.util.UUID;

public record DrawChannelSearchCriteria(UUID tenantId, Boolean activeOnly) {}
