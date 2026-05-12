package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;

public interface OfflineGrantWriterPort {
  OfflineSalesGrantId save(OfflineSalesGrant grant);
  void updateStatus(OfflineSalesGrantId grantId, OfflineSalesGrantStatus status);
}

