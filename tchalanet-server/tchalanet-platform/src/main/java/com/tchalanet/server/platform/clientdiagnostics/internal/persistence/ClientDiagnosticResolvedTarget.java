package com.tchalanet.server.platform.clientdiagnostics.internal.persistence;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

public record ClientDiagnosticResolvedTarget(
    TenantId tenantId, SellerTerminalId sellerTerminalId) {}
