package com.tchalanet.server.core.offlinesync.infra.event;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.application.port.out.SalesOfflineCommandPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SalesOfflineCommandBusAdapter implements SalesOfflineCommandPort {


  @Override
  public SyncResult syncBatch(OfflineBatchId batchId, List<OfflineSaleSubmission> submissions) {
    // Adapter boundary kept explicit; sales command mapping will be finalized in next pass.
    return new SyncResult(0, 0, 0, 0, List.of());
  }
}

