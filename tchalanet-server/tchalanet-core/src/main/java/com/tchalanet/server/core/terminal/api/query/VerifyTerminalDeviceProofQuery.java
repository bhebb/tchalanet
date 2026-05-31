package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record VerifyTerminalDeviceProofQuery(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull TerminalBindingId bindingId,
    @NotNull TerminalProofPurpose purpose,
    @NotBlank String method,
    @NotBlank String path,
    String bodyHash,
    OutletId outletId,
    SalesSessionId sessionId,
    @NotBlank String nonce,
    @NotNull Instant signedAt,
    @NotBlank String signature
) implements Query<VerifyTerminalDeviceProofResult> {}
