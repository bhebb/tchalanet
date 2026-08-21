package com.tchalanet.server.platform.clientdiagnostics.api.model;

import java.time.Instant;
import java.util.Set;

public record ClientDiagnosticPolicyView(
    boolean enabled,
    Instant expiresAt,
    int maxEvents,
    Set<ClientDiagnosticCategory> categories,
    String reason,
    Instant updatedAt) {
  public static ClientDiagnosticPolicyView disabled() {
    return new ClientDiagnosticPolicyView(false, null, 0, Set.of(), null, null);
  }
}
