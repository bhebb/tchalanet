package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "odds")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class OddsJpaEntity extends BaseTenantEntity {

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(name = "multiplier", nullable = false, precision = 12, scale = 4)
  private BigDecimal multiplier;

  @Column(name = "valid_from", nullable = false)
  private Instant validFrom;

  @Column(name = "valid_to")
  private Instant validTo;
}
