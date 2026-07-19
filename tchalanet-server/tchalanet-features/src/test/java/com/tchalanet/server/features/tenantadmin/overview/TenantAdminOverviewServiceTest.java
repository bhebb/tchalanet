package com.tchalanet.server.features.tenantadmin.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.features.tenantadmin.readiness.TenantReadinessAssembler;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAdminOverviewServiceTest {

  private final TenantPreContextLookupApi tenantCatalog = mock(TenantPreContextLookupApi.class);
  private final TenantReadinessAssembler readinessAssembler = mock(TenantReadinessAssembler.class);
  private final AddressApi addressApi = mock(AddressApi.class);
  private final TenantAdminOverviewService service =
      new TenantAdminOverviewService(tenantCatalog, readinessAssembler, addressApi);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());

  @AfterEach
  void clearResponseContext() {
    ApiResponseContext.clear();
  }

  @Test
  void unavailable_header_slices_are_not_reported_as_missing_business_data() {
    when(readinessAssembler.assemble(context())).thenReturn(TenantReadinessView.unknown());
    when(readinessAssembler.computeSetup(TenantReadinessView.unknown()))
        .thenReturn(TenantSetupView.unknown());
    when(addressApi.findPrimaryByTenantId(tenantId))
        .thenThrow(new IllegalStateException("database"));
    when(tenantCatalog.findById(tenantId)).thenThrow(new IllegalStateException("registry"));

    var overview = service.getOverview(context());

    assertThat(overview.header().address()).isNull();
    assertThat(overview.header().addressAvailable()).isFalse();
    assertThat(overview.header().registryAvailable()).isFalse();
    assertThat(ApiResponseContext.get().getNotices())
        .extracting(notice -> notice.code())
        .containsExactlyInAnyOrder(
            "tenantadmin.overview.address_unavailable",
            "tenantadmin.overview.registry_unavailable");
    assertThat(ApiResponseContext.get().getNotices())
        .allSatisfy(notice -> assertThat(notice.message()).isNull());
  }

  private TchRequestContext context() {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        userId.value(),
        Set.of(),
        Set.of(),
        Locale.FRANCE,
        "req-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        tenantId,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        TchActorType.APP_USER,
        null,
        Set.of(),
        Set.of(),
        null);
  }
}
