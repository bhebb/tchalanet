package com.tchalanet.server.core.accesscontrol.application.port.in;

import com.tchalanet.server.core.accesscontrol.domain.model.EffectivePermissions;
import java.util.UUID;

public interface GetEffectivePermissionsUseCase {
  EffectivePermissions getEffectivePermissions(UUID tenantId, UUID userId);
}
