package com.tchalanet.server.platform.tenantgame.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;

/**
 * Query to resolve all enabled games for a tenant.
 * Maps to spec requirement TG2 (resolve effective tenant games).
 * Uses TenantId typed wrapper per typed_ids.md.
 */
@Getter
@Builder
public class ResolveTenantGamesRequest {
  private final TenantId tenantId;
}
