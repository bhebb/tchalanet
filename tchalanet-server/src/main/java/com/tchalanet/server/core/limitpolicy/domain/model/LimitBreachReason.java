package com.tchalanet.server.core.limitpolicy.domain.model;

public record LimitBreachReason(String key, BreachOutcome outcome, String message) {}
