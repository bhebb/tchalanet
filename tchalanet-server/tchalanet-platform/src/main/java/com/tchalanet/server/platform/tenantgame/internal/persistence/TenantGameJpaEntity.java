package com.tchalanet.server.platform.tenantgame.internal.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "tenant_game")
@Getter
@Setter
public class TenantGameJpaEntity extends BaseTenantEntity {

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "game_code", nullable = false, length = 32)
  private String gameCode;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "visible_in_pos", nullable = false)
  private boolean visibleInPos = true;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "display_order", nullable = false)
  private int displayOrder = 0;

  @Column(name = "min_stake", precision = 12, scale = 2)
  private BigDecimal minStake;

  @Column(name = "max_stake", precision = 12, scale = 2)
  private BigDecimal maxStake;

  @Column(name = "availability_enabled", nullable = false)
  private boolean availabilityEnabled = false;

  @Column(name = "availability_days", length = 64)
  private String availabilityDays;

  @Column(name = "start_local_time")
  private LocalTime startLocalTime;

  @Column(name = "end_local_time")
  private LocalTime endLocalTime;
}
