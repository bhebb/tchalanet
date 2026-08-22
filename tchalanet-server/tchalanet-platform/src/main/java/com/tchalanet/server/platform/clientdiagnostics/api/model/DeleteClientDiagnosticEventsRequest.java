package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record DeleteClientDiagnosticEventsRequest(
    @NotEmpty @Size(max = 100) List<UUID> eventIds) {}
