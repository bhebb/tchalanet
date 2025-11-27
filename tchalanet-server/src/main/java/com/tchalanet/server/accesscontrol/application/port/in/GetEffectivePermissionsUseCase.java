package com.tchalanet.server.accesscontrol.application.port.in;

import com.tchalanet.server.accesscontrol.domain.model.EffectivePermissions;
import java.util.UUID;

public interface GetEffectivePermissionsUseCase {
  EffectivePermissions getEffectivePermissions(UUID tenantId, UUID userId);
}
