package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.sales.application.command.model.SyncOfflineSalesCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OfflineSalesPayloadMapper {

  public SyncOfflineSalesCommand toSalesCommand(OfflineBatchId batchId, List<OfflineSaleSubmission> submissions) {
    // TODO parse payloadJson into OfflineTicketSaleInput.
    // Keep raw JSON in offlinesync only. Sales receives typed inputs.
    throw new UnsupportedOperationException("Implement payload parser and map to SyncOfflineSalesCommand");
  }
}
