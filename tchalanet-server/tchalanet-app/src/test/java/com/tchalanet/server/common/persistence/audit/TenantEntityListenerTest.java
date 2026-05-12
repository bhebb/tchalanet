package com.tchalanet.server.common.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantEntityListenerTest {

  private final TenantEntityListener listener = new TenantEntityListener();

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void prePersistAssignsTenantFromCanonicalContext() {
    UUID tenantId = UUID.randomUUID();
    TchContext.set(TchRequestContext.startupTenant(tenantId, "tenant-listener-test"));
    var entity = new TestTenantEntity();

    listener.prePersist(entity);

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void prePersistKeepsAlreadyAssignedTenantForBatchImports() {
    UUID presetTenantId = UUID.randomUUID();
    var entity = new TestTenantEntity();
    entity.setTenantId(presetTenantId);

    listener.prePersist(entity);

    assertThat(entity.getTenantId()).isEqualTo(presetTenantId);
  }

  @Test
  void prePersistFailsFastWhenTenantIsMissing() {
    var entity = new TestTenantEntity();

    assertThatThrownBy(() -> listener.prePersist(entity))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing tenant context");
  }

  @Test
  void preUpdateAllowsMatchingTenant() {
    UUID tenantId = UUID.randomUUID();
    TchContext.set(TchRequestContext.startupTenant(tenantId, "tenant-listener-test"));
    var entity = new TestTenantEntity();
    entity.setTenantId(tenantId);

    assertThatCode(() -> listener.preUpdate(entity)).doesNotThrowAnyException();
  }

  @Test
  void preUpdateRejectsMismatchedTenant() {
    TchContext.set(TchRequestContext.startupTenant(UUID.randomUUID(), "tenant-listener-test"));
    var entity = new TestTenantEntity();
    entity.setTenantId(UUID.randomUUID());

    assertThatThrownBy(() -> listener.preUpdate(entity))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Tenant mismatch on update");
  }

  @Test
  void preUpdateAllowsNoContextForNonRequestWork() {
    var entity = new TestTenantEntity();
    entity.setTenantId(UUID.randomUUID());

    assertThatCode(() -> listener.preUpdate(entity)).doesNotThrowAnyException();
  }

  private static final class TestTenantEntity extends BaseTenantEntity {}
}
