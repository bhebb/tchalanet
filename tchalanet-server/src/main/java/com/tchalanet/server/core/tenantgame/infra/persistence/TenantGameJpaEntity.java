package com.tchalanet.server.core.tenantgame.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaEntity;
import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tenant_game")
@Getter
@Setter
public class TenantGameJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "game_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_tenant_game_game"))
  private GameJpaEntity game;

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
