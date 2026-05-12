package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import java.util.Optional;

public interface OfflineGrantReaderPort {
  Optional<OfflineSalesGrant> findById(OfflineSalesGrantId id);
}

