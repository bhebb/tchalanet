package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tenant_game")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantGameJpaEntity extends BaseTenantEntity {

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = Boolean.TRUE;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "min_stake")
  private BigDecimal minStake;

  @Column(name = "max_stake")
  private BigDecimal maxStake;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "flags", columnDefinition = "jsonb")
  private Map<String, Object> flags;
}
