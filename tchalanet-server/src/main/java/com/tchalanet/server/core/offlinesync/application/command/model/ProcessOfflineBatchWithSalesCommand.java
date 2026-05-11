package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineBatchId;
import jakarta.validation.constraints.NotNull;

public record ProcessOfflineBatchWithSalesCommand(@NotNull OfflineBatchId batchId)
    implements Command<ProcessOfflineBatchWithSalesResult> {}
