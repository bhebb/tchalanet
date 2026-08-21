package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ClientDiagnosticStackFrame(
    @Size(max = 160) String symbol,
    @Size(max = 240) String file,
    @Min(0) Integer line,
    @Min(0) Integer column) {}
