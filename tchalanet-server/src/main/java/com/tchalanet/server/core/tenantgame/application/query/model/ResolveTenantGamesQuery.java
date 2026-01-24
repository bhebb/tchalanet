package com.tchalanet.server.core.tenantgame.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Query to resolve all enabled games for a tenant.
 * Maps to spec requirement TG2 (resolve effective tenant games).
 * Uses TenantId typed wrapper per typed_ids.md.
 */
@Getter
@Builder
public class ResolveTenantGamesQuery implements Query<List<TenantGame>> {
  private final TenantId tenantId;
}
