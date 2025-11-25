package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "limit_policy")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class LimitPolicyJpaEntity extends BaseTenantEntity {

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "target", nullable = false)
  private String target;

  @Column(name = "daily_cap", nullable = false, precision = 14, scale = 2)
  private BigDecimal dailyCap;

  @Column(name = "on_breach", nullable = false)
  private String onBreach;
}
