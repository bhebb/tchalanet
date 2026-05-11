package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;

public interface OfflineGrantWriterPort {
  OfflineSalesGrantId save(OfflineSalesGrant grant);
  void updateStatus(OfflineSalesGrantId grantId, OfflineSalesGrantStatus status);
}

