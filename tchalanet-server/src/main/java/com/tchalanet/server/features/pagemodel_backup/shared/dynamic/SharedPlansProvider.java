package com.tchalanet.server.features.pagemodel_backup.shared.dynamic;

import com.tchalanet.server.catalog.billing.application.query.handler.GetAvailablePlansQueryHandler;
import com.tchalanet.server.catalog.billing.domain.model.Plan;
import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.PlansBlock;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedPlansProvider implements PlansProvider {
  private final GetAvailablePlansQueryHandler plansQueryHandler;

  @Override
  public PlansBlock buildPlansBlock(PageModel pageModel, String currentLang) {
    var plans = plansQueryHandler.handle(null);
    return toPlanBlock(plans);
  }

  private PlansBlock toPlanBlock(List<Plan> plans) {
    if (plans == null || plans.isEmpty()) return new PlansBlock(List.of());

    var items =
        plans.stream().filter(Objects::nonNull).map(PlansBlock.PlanItem::fromDomain).toList();

    return new PlansBlock(items);
  }
}
