package com.tchalanet.server.tenantconfig.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface ResolveTenantQueryHandler {
  Optional<UUID> resolveIdByCode(String tenantCode);
}
