package com.tchalanet.server.platform.clientdiagnostics.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record ClientDiagnosticIngestionContext(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String requestId,
    Instant receivedAtServer) {}
