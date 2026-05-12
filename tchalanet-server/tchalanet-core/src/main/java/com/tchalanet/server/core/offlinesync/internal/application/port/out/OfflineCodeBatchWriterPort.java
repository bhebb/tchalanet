package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineCodeReservation;
import java.util.List;

public interface OfflineCodeBatchWriterPort {
  OfflineCodeBatchId save(OfflineCodeBatch batch);
  void saveReservations(List<OfflineCodeReservation> reservations);
}

