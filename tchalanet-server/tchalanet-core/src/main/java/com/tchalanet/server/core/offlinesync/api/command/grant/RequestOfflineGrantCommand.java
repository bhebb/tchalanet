package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * POS asks the server to issue a fresh offline grant and code batch.
 * Trusted operational context required upstream.
 */
public record RequestOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull UserId sellerUserId,
    @NotNull TerminalId terminalId,
    @NotNull OutletId outletId,
    @NotNull SalesSessionId salesSessionId,
    @NotNull UUID deviceId,
    @NotBlank String devicePublicKey,
    @NotBlank String keyId
) implements Command<RequestOfflineGrantResult> {}
