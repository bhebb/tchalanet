package com.tchalanet.server.draw.application.query.model;

import java.util.UUID;

public record DrawChannelSearchCriteria(UUID tenantId, Boolean activeOnly) {}
