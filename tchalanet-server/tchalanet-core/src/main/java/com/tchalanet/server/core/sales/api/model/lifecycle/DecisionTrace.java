package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record DecisionTrace(Instant at, UserId by, String reason) {}

