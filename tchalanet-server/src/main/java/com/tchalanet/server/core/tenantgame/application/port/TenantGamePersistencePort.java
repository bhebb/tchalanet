package com.tchalanet.server.core.tenantgame.application.port;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import java.util.List;
import java.util.Optional;

/**
 * Port for TenantGame persistence operations.
 * All methods use TenantId typed wrapper per typed_ids.md.
 */
public interface TenantGamePersistencePort {
  TenantGame save(TenantGame tenantGame);
  Optional<TenantGame> findByTenantIdAndGameCode(TenantId tenantId, String gameCode);
  List<TenantGame> findAllByTenantId(TenantId tenantId);
}
