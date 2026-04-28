package com.tchalanet.server.catalog.drawchannel.internal.persistence;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
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
            columnNames = {"tenant_id", "draw_channel_id", "game_id"}),
    indexes = {
      @Index(name = "ix_dcg_tenant_channel", columnList = "tenant_id, draw_channel_id"),
      @Index(name = "ix_dcg_tenant_game", columnList = "tenant_id, game_id")
    })
@Getter
@Setter
public class DrawChannelGameEntity extends BaseTenantEntity {

  @Column(name = "draw_channel_id", nullable = false)
  private UUID drawChannelId;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode flags;
}
