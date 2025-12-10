package com.tchalanet.server.core.billing.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import com.tchalanet.server.core.billing.domain.model.BillingProvider;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "subscription")
@Audited
@Getter
@Setter
public class SubscriptionJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionStatus status;

  @Column(name = "current_period_start")
  private Instant currentPeriodStart;

  @Column(name = "current_period_end")
  private Instant currentPeriodEnd;

  @Column(name = "cancel_at_period_end", nullable = false)
  private boolean cancelAtPeriodEnd;

  @Column(name = "billing_provider", length = 16)
  @Enumerated(EnumType.STRING)
  private BillingProvider billingProvider;

  @Column(name = "billing_external_id", length = 128)
  private String billingExternalId;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "meta", columnDefinition = "jsonb")
  private Map<String, Object> meta;

  // --- Méthodes métier (OO) recommandées : ---
  public void scheduleCancelAtPeriodEnd() {
    this.cancelAtPeriodEnd = true;
  }

  public void cancelNow() {
    this.status = SubscriptionStatus.CANCELED;
    this.cancelAtPeriodEnd = false;
  }

  public void changePlan(com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity newPlan) {
    this.plan = newPlan; /* autres règles éventuelles */
  }

  // getters/setters...
}

