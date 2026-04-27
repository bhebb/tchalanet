package com.tchalanet.server.core.autonomy.application.query.model;

import java.util.UUID;

public record AutonomyMeta(
    boolean configured,
    boolean deleted,
    UUID ruleId,
    long version) {}
