package com.tchalanet.server.platform.accesscontrol.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.model.StartSupportAccessSessionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.SupportAccessSessionView;
import java.util.Optional;

public interface SupportAccessApi {
  SupportAccessSessionView start(StartSupportAccessSessionRequest request);

  Optional<SupportAccessSessionView> current(UserId superAdminUserId);

  void stop(UserId superAdminUserId);

  Optional<TenantId> currentTenant(UserId superAdminUserId);
}
