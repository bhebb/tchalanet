package com.tchalanet.server.core.offlinesync.application.command.model.batch;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.offlinesync.application.command.model.OfflineSaleSubmissionInput;
import java.util.List;

public record ReceiveOfflineBatchCommand(
    TenantId tenantId,
    TerminalId terminalId,
    OfflineSalesGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String clientBatchId,
    List<OfflineSaleSubmissionInput> submissions
) implements Command<ReceiveOfflineBatchResult> {}

