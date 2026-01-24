package com.tchalanet.server.core.tenantgame.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity for tenant-scoped game configuration.
 * Stores game_id as UUID (FK reference to catalog/game, not as JPA relation).
 * Per inter_domain_calls.md: core/tenantgame depends on GameCatalog API only,
 * not on catalog/game internal entities (GameJpaEntity).
 * Validation of game existence happens at application layer via GameCatalog.
 */
@Entity
@Table(name = "tenant_game")
@Getter
@Setter
public class TenantGameJpaEntity extends BaseTenantEntity {

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "min_stake", precision = 12, scale = 2)
  private BigDecimal minStake;

  @Column(name = "max_stake", precision = 12, scale = 2)
  private BigDecimal maxStake;

  @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode flags;
}
