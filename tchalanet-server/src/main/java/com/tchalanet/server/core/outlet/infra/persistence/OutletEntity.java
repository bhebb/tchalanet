package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "outlet")
public class OutletEntity extends BaseTenantEntity {

  @Id
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "slug", nullable = false)
  private String slug;

  public OutletEntity() {}

  public OutletEntity(Outlet o) {
    this.id = o.id();
    this.name = o.name();
    this.slug = o.slug();
  }

  public Outlet toDomain() {
    return new Outlet(id, name, slug);
  }

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }
}

