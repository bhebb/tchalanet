package com.tchalanet.server.core.billing.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import com.tchalanet.server.core.billing.domain.model.BillingFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "plan")
@Getter
@Setter
@Audited
public class PlanJpaEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "price_amount", nullable = false)
  private BigDecimal priceAmount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "billing_frequency", nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  private BillingFrequency billingFrequency;

  @Column(name = "public_plan", nullable = false)
  private boolean publicPlan;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "features", columnDefinition = "jsonb")
  private Map<String, Object> features;

  // inverse: subscriptions referencing this plan
  @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
  private List<com.tchalanet.server.core.billing.infra.persistence.SubscriptionJpaEntity> subscriptions = new ArrayList<>();
}

