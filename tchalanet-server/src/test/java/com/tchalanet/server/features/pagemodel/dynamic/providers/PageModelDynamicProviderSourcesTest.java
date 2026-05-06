package com.tchalanet.server.features.pagemodel.dynamic.providers;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageModelDynamicProviderSourcesTest {

  @Test
  void providersSupportExactSnakeCaseSources() {
    List<PageModelDynamicProvider> providers =
        List.of(
            new PublicNewsProvider(null),
            new PublicDrawResultsProvider(null),
            new PublicFeaturesProvider(),
            new PublicTchalaProvider(),
            new PublicTestimonialsProvider(),
            new PlansProvider(null),
            new CashierOverviewProvider(null),
            new CashierQuickSaleProvider(),
            new CashierRecentTicketsProvider(),
            new CashierSessionProvider(),
            new CashierNextDrawsProvider(),
            new CashierLimitsProvider(),
            new AdminKpisProvider(),
            new AdminDrawOperationsProvider(),
            new AdminApprovalQueueProvider(),
            new AdminAlertsProvider(),
            new SuperAdminSystemHealthProvider(),
            new SuperAdminBatchStatusProvider(),
            new SuperAdminTenantsProvider(),
            new SuperAdminVersionProvider());

    assertThat(providers)
        .allSatisfy(provider -> assertThat(provider.supports("private.dashboard.cashier", null, provider.providerKey())).isTrue());
  }
}
