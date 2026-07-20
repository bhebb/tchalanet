package com.tchalanet.server.features.reporting.adminreports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdminReportsServiceTest {

  @AfterEach
  void clearResponseContext() {
    ApiResponseContext.clear();
  }

  @Test
  void doesNotExposeEmptyReportTotalsWhenAnalyticsIsUnavailable() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    AdminReportPeriodRequest request =
        new AdminReportPeriodRequest(
            LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 7, 19),
            ZoneId.of("America/Port-au-Prince"),
            100,
            100,
            List.of(),
            List.of());
    AnalyticsTrustScope scope =
        AnalyticsTrustScope.tenant(tenantId, request.from(), request.to());
    QueryBus queryBus = mock(QueryBus.class);
    when(queryBus.ask(any(GetAnalyticsTrustStateQuery.class)))
        .thenReturn(
            AnalyticsTrustStateView.unavailable(
                scope, List.of(request.from()), Instant.now()));

    AdminReportResponses.Overview response = new AdminReportsService(queryBus).overview(tenantId, request);

    assertThat(response.analytics().available()).isFalse();
    assertThat(response.analytics().trustState()).isEqualTo("UNAVAILABLE");
    assertThat(response.summary()).isNull();
    assertThat(response.dailyRows()).isEmpty();
    assertThat(ApiResponseContext.get().getNotices())
        .singleElement()
        .extracting(notice -> notice.code())
        .isEqualTo("reporting.analytics.unavailable");
    verify(queryBus).ask(new GetAnalyticsTrustStateQuery(scope));
    verify(queryBus, never()).ask(any(GetTenantFinancialBreakdownQuery.class));
  }
}
