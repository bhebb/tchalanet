package com.tchalanet.server.features.ops.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.features.ops.error.OpsErrorCodes;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpsBatchLaunchFacadeTest {

  private final BatchJobStarter batchJobStarter = mock(BatchJobStarter.class);
  private final TenantPreContextLookupApi tenantLookup = mock(TenantPreContextLookupApi.class);
  private final OpsBatchLaunchFacade facade =
      new OpsBatchLaunchFacade(batchJobStarter, tenantLookup);

  @Test
  void rejectsUnknownTenantCodeWithStableCode() {
    when(tenantLookup.findByCode("missing")).thenReturn(Optional.empty());

    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                facade.launchForTenants(
                    JobKey.of("test:batch:launch"), List.of("missing"), tenantId -> Map.of()),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_UNKNOWN_TENANT_CODE.code());
  }

  @Test
  void rejectsTooManyTenantLaunchesWithDeclaredPublicParams() throws Exception {
    setMaxTenantsPerInteractiveLaunch(1);
    when(tenantLookup.listActiveTenantIds())
        .thenReturn(
            List.of(
                TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
                TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000002"))));

    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                facade.launchForTenants(
                    JobKey.of("test:batch:launch"), List.of(), tenantId -> Map.of()),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_TOO_MANY_TENANTS.code())
        .containsEntry("params", Map.of("requestedTenants", 2, "maxTenants", 1));
  }

  private void setMaxTenantsPerInteractiveLaunch(int value) throws Exception {
    var field = OpsBatchLaunchFacade.class.getDeclaredField("maxTenantsPerInteractiveLaunch");
    field.setAccessible(true);
    field.setInt(facade, value);
  }
}
