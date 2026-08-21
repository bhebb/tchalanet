package com.tchalanet.server.platform.clientdiagnostics.api.model;

import java.time.Instant;

public record ClientDiagnosticIngestionResult(
    ClientDiagnosticIngestionStatus status,
    int received,
    int accepted,
    int ignored,
    Instant receivedAtServer) {}
