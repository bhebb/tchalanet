package com.tchalanet.server.platform.clientdiagnostics.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Set;

public record ClientDiagnosticDebugSessionView(
    TenantId tenantId,
    String tenantCode,
    String tenantName,
    SellerTerminalId sellerTerminalId,
    String terminalCode,
    String terminalName,
    Instant expiresAt,
    int maxEvents,
    Set<ClientDiagnosticCategory> categories,
    String reason,
    Instant updatedAt,
    long eventCount,
    Instant lastEventAt,
    ClientDiagnosticSeverity lastSeverity,
    ClientDiagnosticCategory lastCategory) {}
