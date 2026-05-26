package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * E.2 — Provider des plans actifs.
 * Source : "public_plans"
 */
@Component
@RequiredArgsConstructor
public class PlansProvider implements PageModelDynamicProvider {

  private final PlanCatalog planCatalog;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "public_plans".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext resolutionContext) {
    try {
      List<Map<String, Object>> plans =
          planCatalog.listActive().stream()
              .map(this::toMap)
              .toList();
      return Map.of("plans", plans);
    } catch (Exception e) {
      return Map.of("plans", List.of());
    }
  }

  private Map<String, Object> toMap(PlanView plan) {
    return Map.of(
        "value", plan.code() != null ? plan.code() : "",
        "name", plan.name() != null ? plan.name() : "",
        "description", plan.description() != null ? plan.description() : "",
        "price", plan.priceAmount() != null ? plan.priceAmount() : 0,
        "currency", plan.currency() != null ? plan.currency() : "",
        "billingPeriod", plan.billingPeriod() != null ? plan.billingPeriod() : "",
        "features", plan.featuresJson() != null ? plan.featuresJson() : Map.of(),
        "isDefault", plan.isDefault());
  }

  @Override
  public String providerKey() {
    return "public_plans";
  }
}
