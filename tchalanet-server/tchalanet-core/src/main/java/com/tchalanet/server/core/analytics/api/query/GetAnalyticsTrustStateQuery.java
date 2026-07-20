package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import java.util.Objects;

/** Checks whether a requested analytics scope has a trustworthy projection coverage. */
public record GetAnalyticsTrustStateQuery(AnalyticsTrustScope scope)
    implements Query<AnalyticsTrustStateView> {

  public GetAnalyticsTrustStateQuery {
    Objects.requireNonNull(scope, "scope is required");
  }
}
