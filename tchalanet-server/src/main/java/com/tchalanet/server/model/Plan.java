package com.tchalanet.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plans")
@Getter
@Setter
public class Plan {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true)
  private String code; // "PRO", "ENTERPRISE"

  @Column(nullable = false)
  private BigDecimal priceAmount;

  @Column(nullable = false)
  private String currency; // "EUR", "USD"

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = "billing_frequency")
  private BillingFrequency frequency; // MONTH, YEAR

  @Column(nullable = false)
  private boolean publicPlan = false;

  //    @Convert(converter = StringListJsonConverter.class)
  @Column(columnDefinition = "jsonb")
  private List<String> features; // "plans.feat.analytics", ...
  /*
  @Version
  private long version;*/

}
