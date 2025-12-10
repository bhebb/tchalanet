package com.tchalanet.server.core.outlet.domain.model;

import java.util.UUID;

public final class Outlet {
  private final UUID id;
  private final UUID tenantId;
  private final String name;
  private final String slug;

  public Outlet(UUID id, UUID tenantId, String name, String slug) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.slug = slug;
  }

  public UUID id() { return id; }
  public UUID tenantId() { return tenantId; }
  public String name() { return name; }
  public String slug() { return slug; }
}
