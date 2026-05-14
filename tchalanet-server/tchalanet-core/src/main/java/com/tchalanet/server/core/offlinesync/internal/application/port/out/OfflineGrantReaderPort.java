package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
import java.util.Optional;

public interface OfflineGrantReaderPort {
  Optional<OfflineSalesGrant> findById(OfflineSalesGrantId id);

  boolean existsForFrame(
      TenantId tenantId,
      UserId sellerUserId,
      TerminalId terminalId,
      SalesSessionId salesSessionId,
      OfflineSalesGrantStatus status);
}
