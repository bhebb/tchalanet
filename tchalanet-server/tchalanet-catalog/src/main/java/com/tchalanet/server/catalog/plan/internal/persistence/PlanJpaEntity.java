package com.tchalanet.server.catalog.plan.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import com.tchalanet.server.common.persistence.BaseEntity;

import java.math.BigDecimal;

/**
 * JPA entity for billing_plan table (catalog/plan).
 * Maps to spec requirement P1-P5 (catalog-plan spec).
 * Pure reference data, no lifecycle, no events.
 */
@Entity
@Table(name = "billing_plan")
@Getter
@Setter
@Audited
public class PlanJpaEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 128)
  private String code;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "price_amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal priceAmount = BigDecimal.ZERO;

  @Column(name = "currency", length = 3, nullable = false)
  private String currency = "USD";

  @Column(name = "billing_period", length = 50, nullable = false)
  private String billingPeriod = "MONTHLY";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "limits_json", columnDefinition = "jsonb")
  private String limitsJson; // stored as JSON string

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "features_json", columnDefinition = "jsonb")
  private String featuresJson; // stored as JSON string

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "is_default", nullable = false)
  private boolean defaultPlan = false;
}
