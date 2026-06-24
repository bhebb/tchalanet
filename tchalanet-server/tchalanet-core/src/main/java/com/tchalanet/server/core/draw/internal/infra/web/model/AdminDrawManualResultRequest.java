package com.tchalanet.server.core.draw.internal.infra.web.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record AdminDrawManualResultRequest(
    @Size(max = 255) String recordedBy,
    @Size(max = 500) String notes,
    @Size(max = 20) String pick3,
    @Size(max = 20) String pick4,
    boolean force,
    @Size(max = 500) String reason,
    boolean observeTrustPolicy
) {
    @AssertTrue(message = "reason is required when force is true")
    public boolean isReasonValidForForce() {
        return !force || (reason != null && !reason.isBlank());
    }
}
