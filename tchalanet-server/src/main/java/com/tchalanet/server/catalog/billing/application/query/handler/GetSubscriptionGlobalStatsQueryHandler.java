package com.tchalanet.server.catalog.billing.application.query.handler;

import com.tchalanet.server.catalog.billing.application.query.model.GetSubscriptionGlobalStatsQuery;
import com.tchalanet.server.catalog.billing.application.query.model.SubscriptionGlobalStatsView;
import com.tchalanet.server.catalog.billing.application.query.model.SubscriptionGlobalStatsView.ByPlanItem;
import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import com.tchalanet.server.catalog.billing.infra.persistence.repo.SubscriptionJpaRepository;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSubscriptionGlobalStatsQueryHandler
    implements QueryHandler<GetSubscriptionGlobalStatsQuery, SubscriptionGlobalStatsView> {

  private final SubscriptionJpaRepository repo;

  @Override
  public SubscriptionGlobalStatsView handle(GetSubscriptionGlobalStatsQuery query) {
    long total = repo.countByDeletedAtIsNull();
    long active = repo.countByStatusAndDeletedAtIsNull(SubscriptionStatus.ACTIVE);
    long pastDue = repo.countByStatusAndDeletedAtIsNull(SubscriptionStatus.PAST_DUE);
    long canceled = repo.countByStatusAndDeletedAtIsNull(SubscriptionStatus.CANCELED);

    List<ByPlanItem> byPlan = new ArrayList<>();
    List<Object[]> planCounts = repo.countByPlanGrouped();
    for (Object[] row : planCounts) {
      String planCode = (String) row[0];
      long planTotal = ((Number) row[1]).longValue();
      long planActive = ((Number) row[2]).longValue();
      byPlan.add(new ByPlanItem(planCode, (int) planTotal, (int) planActive));
    }

    return new SubscriptionGlobalStatsView(
        (int) total, (int) active, (int) pastDue, (int) canceled, byPlan);
  }
}
