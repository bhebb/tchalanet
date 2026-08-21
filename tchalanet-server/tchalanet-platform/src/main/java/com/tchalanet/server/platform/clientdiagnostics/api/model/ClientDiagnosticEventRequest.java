package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record ClientDiagnosticEventRequest(
    @NotBlank @Size(max = 96) String eventId,
    @NotNull ClientDiagnosticCategory category,
    @NotNull Instant occurredAtClient,
    @NotNull ClientDiagnosticSeverity severity,
    @NotBlank @Size(max = 96) String operation,
    @Size(max = 128) String errorCode,
    @Size(max = 512) String message,
    @Size(max = 160) String exceptionType,
    @Size(max = 128) String requestId,
    @Size(max = 128) String correlationId,
    Integer httpStatus,
    @Size(max = 128) String endpointKey,
    @Size(max = 48) String appVersion,
    @Size(max = 48) String buildNumber,
    @Size(max = 32) String platform,
    @Size(max = 96) String deviceModel,
    @Size(max = 96) String osVersion,
    @Size(max = 96) String printerProvider,
    @Size(max = 160) String printerService,
    @Size(max = 96) String printerState,
    @Size(max = 24) List<@Valid ClientDiagnosticStackFrame> stackFrames) {

  public ClientDiagnosticEventRequest {
    if (httpStatus != null && (httpStatus < 100 || httpStatus > 599)) {
      throw new IllegalArgumentException("httpStatus must be between 100 and 599");
    }
  }
}
