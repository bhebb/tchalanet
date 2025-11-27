package com.tchalanet.server.tenant.domain.model;

import java.util.UUID;

public class TenantUser {
  private UUID id;
  private UUID tenantId;
  private UUID userId;

  // getters/setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID t) {
    this.tenantId = t;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID u) {
    this.userId = u;
  }
}
