package com.tchalanet.server.core.game.internal.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity
@Table(
    name = "tenant_game",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_tenant_game",
            columnNames = {"tenant_id", "game_id"}),
    indexes = {
      @Index(name = "ix_tenant_game_tenant_enabled", columnList = "tenant_id, enabled"),
      @Index(name = "ix_tenant_game_game", columnList = "game_id")
    })
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
