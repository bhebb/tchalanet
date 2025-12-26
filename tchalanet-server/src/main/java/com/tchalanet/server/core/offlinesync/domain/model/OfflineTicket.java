package com.tchalanet.server.core.offlinesync.domain.model;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record OfflineTicket(
    UUID id, TenantId tenantId, TerminalId terminalId, String payloadJson, Instant receivedAt) {}
