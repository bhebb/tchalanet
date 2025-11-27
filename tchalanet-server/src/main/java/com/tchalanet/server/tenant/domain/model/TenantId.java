package com.tchalanet.server.tenant.domain.model;

import java.util.UUID;

public final class TenantId {
  private final UUID id;

  public TenantId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID value() {
    return id;
  }

  @Override
  public String toString() {
    return id == null ? null : id.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TenantId tenantId = (TenantId) o;
    return id.equals(tenantId.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
