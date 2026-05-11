package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReceiveOfflineBatchCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull OfflineSalesGrantId grantId,
    @NotNull OfflineCodeBatchId codeBatchId,
    @NotBlank String clientBatchId,
    @NotEmpty List<@Valid OfflineSaleSubmissionInput> submissions
) implements Command<ReceiveOfflineBatchResult> {}
