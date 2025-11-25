package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import com.tchalanet.server.common.infra.persistence.MapToJsonConverter;
import com.tchalanet.server.pos.infra.persistence.TicketJpaEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "draw")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class DrawJpaEntity extends BaseTenantEntity {

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(name = "draw_channel_id", nullable = false)
  private java.util.UUID drawChannelId;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(name = "cutoff_sec", nullable = false)
  private Integer cutoffSec;

  @Column(name = "status", nullable = false)
  private String status;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "result_payload", columnDefinition = "jsonb")
  private Map<String, Object> resultPayload;

  @Column(name = "draw_source")
  private String drawSource;

  @Column(name = "system_generated", nullable = false)
  private Boolean systemGenerated = Boolean.TRUE;

  @Column(name = "locked", nullable = false)
  private Boolean locked = Boolean.FALSE;

  @OneToMany(mappedBy = "draw", fetch = FetchType.LAZY)
  private List<TicketJpaEntity> tickets = new ArrayList<>();
}
