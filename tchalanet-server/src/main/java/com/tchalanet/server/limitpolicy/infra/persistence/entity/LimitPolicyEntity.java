package com.tchalanet.server.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.limitpolicy.domain.model.LimitScope;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "limit_policy")
@Getter
@Setter
public class LimitPolicyEntity extends BaseTenantEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LimitScope scope;

  @Column private String target; // e.g., gameCode, terminalId, userId

  @Column(name = "daily_cap")
  private BigDecimal dailyCap;

  @Column(name = "max_stake_per_line")
  private BigDecimal maxStakePerLine;

  @Column(name = "max_payout_per_line")
  private BigDecimal maxPayoutPerLine;

  @Enumerated(EnumType.STRING)
  @Column(name = "on_breach", nullable = false)
  private BreachOutcome onBreach;

  @Column(nullable = false)
  private boolean active;
}
