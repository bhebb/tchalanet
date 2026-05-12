package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineBatchId;

public record ProcessOfflineBatchWithSalesCommand(OfflineBatchId batchId)
    implements Command<ProcessOfflineBatchWithSalesResult> {}

