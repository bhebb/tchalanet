package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;
import java.util.Optional;

public interface OfflineGrantReaderPort {
  Optional<OfflineSalesGrant> findById(OfflineSalesGrantId id);
}

