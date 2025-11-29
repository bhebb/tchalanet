package com.tchalanet.server.core.tenant.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "game")
@Getter
@Setter
public class GameEntity
    extends BaseEntity { // Assuming game is not tenant-specific, or BaseTenantEntity if it is
  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  // ... other fields from your game table definition
}
