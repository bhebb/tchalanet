package com.tchalanet.server.core.autonomy.domain.ids;

import java.util.UUID;

public record AutonomyTargetId(UUID value) {
    public static AutonomyTargetId of(UUID id) { return new AutonomyTargetId(id); }
}
