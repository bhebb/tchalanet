package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;

public record LimitBreachReason(String key, BreachOutcome outcome, String message) {}
