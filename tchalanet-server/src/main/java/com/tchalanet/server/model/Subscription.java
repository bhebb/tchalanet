package com.tchalanet.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String tenantId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Plan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status; // ACTIVE, TRIALING, CANCELED, PAST_DUE, SUSPENDED

  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;

  private boolean cancelAtPeriodEnd = false;

  @Enumerated(EnumType.STRING)
  private BillingProvider billingProvider; // STRIPE, ADYEN, NONE

  private String billingExternalId; // sub id provider

  //  @Convert(converter = JsonMapConverter.class)
  @Transient
  //  @Column(columnDefinition = "jsonb")
  private Map<String, Object> meta;

  @Version private long version;

  // --- Méthodes métier (OO) recommandées : ---
  public void scheduleCancelAtPeriodEnd() {
    this.cancelAtPeriodEnd = true;
  }

  public void cancelNow() {
    this.status = SubscriptionStatus.CANCELED;
    this.cancelAtPeriodEnd = false;
  }

  public void changePlan(Plan newPlan) {
    this.plan = newPlan; /* autres règles éventuelles */
  }

  // getters/setters...
}
