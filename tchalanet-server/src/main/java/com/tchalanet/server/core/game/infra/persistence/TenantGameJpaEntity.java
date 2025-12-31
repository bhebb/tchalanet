package com.tchalanet.server.core.game.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "tenant_game",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "game_id"})})
@Audited
@Getter
@Setter
public class TenantGameJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "game_id", nullable = false)
  private GameJpaEntity game;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "min_stake", precision = 12, scale = 2)
  private BigDecimal minStake;

  @Column(name = "max_stake", precision = 12, scale = 2)
  private BigDecimal maxStake;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "flags", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> flags = Collections.emptyMap();
}
