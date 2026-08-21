package com.tchalanet.server.platform.clientdiagnostics.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record ClientDiagnosticEventView(
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
    String platform,
    String deviceModel,
    String printerProvider,
    String printerState) {}
