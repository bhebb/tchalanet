package com.tchalanet.server.core.draw.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity
@Table(
    name = "draw_channel_game",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_draw_channel_game",
            columnNames = {"tenantId", "draw_channel_id", "game_id"}),
    indexes = {
      @Index(name = "ix_dcg_tenant_channel", columnList = "tenantId, draw_channel_id"),
      @Index(name = "ix_dcg_tenant_game", columnList = "tenantId, game_id")
    })
@Getter
@Setter
public class DrawChannelGameJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "draw_channel_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_dcg_channel"))
  private DrawChannelJpaEntity drawChannel;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode flags;
}
