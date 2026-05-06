package com.tchalanet.server.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TchContextRunnerTest {

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void runAsTenantResultClearsContextWhenNoPreviousContextExists() {
    var tenantId = UUID.randomUUID();

    var result =
        TchContextRunner.runAsTenantResult(
            tenantId,
            "test",
            () -> TchContext.currentOrNull().tenantIdSafe());

    assertThat(result).isEqualTo(TenantId.of(tenantId));
    assertThat(TchContext.currentOrNull()).isNull();
  }

  @Test
  void runAsTenantResultRestoresPreviousContext() {
    var previousTenantId = UUID.randomUUID();
    var temporaryTenantId = UUID.randomUUID();
    var previous = TchRequestContext.startupTenant(previousTenantId, "previous");
    TchContext.set(previous);

    var result =
        TchContextRunner.runAsTenantResult(
            temporaryTenantId,
            "temporary",
            () -> TchContext.currentOrNull().tenantIdSafe());

    assertThat(result).isEqualTo(TenantId.of(temporaryTenantId));
    assertThat(TchContext.currentOrNull()).isSameAs(previous);
    assertThat(TchContext.currentOrNull().tenantIdSafe()).isEqualTo(TenantId.of(previousTenantId));
  }

  @Test
  void runAsTenantRestoresPreviousContext() {
    var previousTenantId = UUID.randomUUID();
    var previous = TchRequestContext.startupTenant(previousTenantId, "previous");
    TchContext.set(previous);

    TchContextRunner.runAsTenant(UUID.randomUUID(), "temporary", () -> {});

    assertThat(TchContext.currentOrNull()).isSameAs(previous);
  }

  @Test
  void explicitStartupTenantScopeRestoresPreviousContext() {
    var previousTenantId = UUID.randomUUID();
    var startupTenantId = UUID.randomUUID();
    var previous = TchRequestContext.startupTenant(previousTenantId, "previous");
    TchContext.set(previous);

    var result =
        TchContextScope.runStartupTenantResult(
            startupTenantId,
            "startup",
            () -> TchContext.currentOrNull().tenantIdSafe());

    assertThat(result).isEqualTo(TenantId.of(startupTenantId));
    assertThat(TchContext.currentOrNull()).isSameAs(previous);
  }

  @Test
  void currentOrThrowFailsWhenContextIsMissing() {
    assertThatThrownBy(TchContext::currentOrThrow)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing TchRequestContext");
  }

  @Test
  void requestContextExposesClearTenantAccessors() {
    var tenantId = UUID.randomUUID();
    var ctx = TchRequestContext.startupTenant(tenantId, "test");

    assertThat(ctx.effectiveTenantIdOrNull()).isEqualTo(TenantId.of(tenantId));
    assertThat(ctx.effectiveTenantIdRequired()).isEqualTo(TenantId.of(tenantId));
    assertThat(ctx.hasTenant()).isTrue();
    assertThat(ctx.isPlatformScope()).isFalse();
  }
}
