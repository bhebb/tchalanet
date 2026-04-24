package com.tchalanet.server.features.pagemodel_backup.shared.block;

import com.tchalanet.server.core.subscription.domain.model.BillingFrequency;
import com.tchalanet.server.catalog.billing.domain.model.Plan;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PlansBlock(List<PlanItem> items) {

  public record PlanItem(
      String name,
      String description,
      BigDecimal priceAmount,
      BillingFrequency billingFrequency,
      Map<String, Object> featureKeys // ou List<PlanFeatureItem>
      ) {
    public static PlanItem fromDomain(Plan plan) {
      Map<String, Object> featureKeys = plan.features() != null ? plan.features() : Map.of();
      return new PlanItem(
          plan.name(),
          plan.description(),
          plan.priceAmount(),
          plan.billingFrequency(),
          featureKeys);
    }
  }
}
