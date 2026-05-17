package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReceiveOfflineBatchCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSalesGrantId grantId,
    @NotNull String deviceSignature,
    @NotNull @NotEmpty List<OfflineSaleSubmissionPayload> submissions
) implements Command<OfflineBatchId> {

    public record OfflineSaleSubmissionPayload(
        @NotNull String clientSaleId,
        @NotNull String payload
    ) {}
}
