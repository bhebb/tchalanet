package com.tchalanet.server.common.batch.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class BatchTchContextBinderTest {

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void bindWithoutTenantCreatesPlatformContext() {
    var binder = new BatchTchContextBinder(tenantId -> Optional.empty());
    var params =
        new JobParametersBuilder()
            .addString(BatchParamKeys.REQUEST_ID, "batch-platform-1")
            .addString(BatchParamKeys.ACTOR, "scheduler")
            .toJobParameters();

    binder.bind(params);

    var ctx = TchContext.currentOrNull();
    assertThat(ctx).isNotNull();
    assertThat(ctx.apiScope()).isEqualTo(ApiScope.PLATFORM);
    assertThat(ctx.tenantIdSafe()).isNull();
    assertThat(ctx.requestId()).isEqualTo("batch-platform-1");
    assertThat(ctx.keycloakUserId()).isEqualTo("scheduler");
    assertThat(ctx.systemRoles()).containsExactly(TchRole.SYSTEM);
  }

  @Test
  void bindWithTenantCreatesTenantContextFromBootstrapProvider() {
    UUID tenantUuid = UUID.randomUUID();
    TenantId tenantId = TenantId.of(tenantUuid);
    var binder =
        new BatchTchContextBinder(
            requestedTenant ->
                requestedTenant.equals(tenantId)
                    ? Optional.of(
                        new BatchTenantBootstrap(
                            tenantId,
                            "tenant-demo",
                            ZoneId.of("America/Port-au-Prince"),
                            Currency.getInstance("HTG")))
                    : Optional.empty());
    var params =
        new JobParametersBuilder()
            .addString(BatchParamKeys.TENANT_ID, tenantUuid.toString())
            .addString(BatchParamKeys.REQUEST_ID, "batch-tenant-1")
            .toJobParameters();

    binder.bind(params);

    var ctx = TchContext.currentOrNull();
    assertThat(ctx).isNotNull();
    assertThat(ctx.apiScope()).isEqualTo(ApiScope.TENANT);
    assertThat(ctx.tenantIdSafe()).isEqualTo(tenantId);
    assertThat(ctx.tenantZoneId()).isEqualTo(ZoneId.of("America/Port-au-Prince"));
    assertThat(ctx.tenantCurrency()).isEqualTo(Currency.getInstance("HTG"));
    assertThat(ctx.requestId()).isEqualTo("batch-tenant-1");
    assertThat(ctx.systemRoles()).containsExactly(TchRole.SYSTEM);
  }

  @Test
  void bindWithUnknownTenantFailsFast() {
    var binder = new BatchTchContextBinder(tenantId -> Optional.empty());
    var params =
        new JobParametersBuilder()
            .addString(BatchParamKeys.TENANT_ID, UUID.randomUUID().toString())
            .toJobParameters();

    assertThatThrownBy(() -> binder.bind(params))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown tenant_id");
  }

  @Test
  void clearRemovesCurrentContext() {
    var binder = new BatchTchContextBinder(tenantId -> Optional.empty());
    binder.bind(new JobParametersBuilder().toJobParameters());

    binder.clear();

    assertThat(TchContext.currentOrNull()).isNull();
  }
}
