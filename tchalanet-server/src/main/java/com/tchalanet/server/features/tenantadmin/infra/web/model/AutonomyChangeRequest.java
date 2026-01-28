package com.tchalanet.server.features.tenantadmin.infra.web.model;

import com.tchalanet.server.common.types.enums.AutonomyLevel;

public record AutonomyChangeRequest(AutonomyLevel autonomyLevel) {}
