package com.tchalanet.server.features.pos.profile.model;

import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticCategory;
import java.time.Instant;
import java.util.Set;

public record PosProfileDiagnosticsInfo(
    boolean enabled, Instant expiresAt, int maxEvents, Set<ClientDiagnosticCategory> categories) {}
