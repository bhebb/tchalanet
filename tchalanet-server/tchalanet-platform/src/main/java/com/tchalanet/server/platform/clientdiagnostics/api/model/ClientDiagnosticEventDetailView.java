package com.tchalanet.server.platform.clientdiagnostics.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;

public record ClientDiagnosticEventDetailView(
    String id,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String eventId,
    ClientDiagnosticCategory category,
    ClientDiagnosticSeverity severity,
    String operation,
    Instant occurredAtClient,
    Instant receivedAtServer,
    String requestId,
    String correlationId,
    String errorCode,
    String message,
    String exceptionType,
    Integer httpStatus,
    String endpointKey,
    String appVersion,
    String buildNumber,
    String platform,
    String deviceModel,
    String osVersion,
    String printerProvider,
    String printerService,
    String printerState,
    List<ClientDiagnosticStackFrame> stackFrames) {}
