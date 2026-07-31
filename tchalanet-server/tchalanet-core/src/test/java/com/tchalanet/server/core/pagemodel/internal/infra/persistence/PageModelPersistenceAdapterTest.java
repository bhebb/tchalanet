package com.tchalanet.server.core.pagemodel.internal.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PageModelPersistenceAdapterTest {

  private static final UUID PAGE_MODEL_UUID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID TENANT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID ACTOR_UUID = UUID.fromString("40000000-0000-0000-0000-000000000001");

  @Test
  void saveAllReusesManagedRowsForExistingInstances() {
    var repository = mock(PageModelJpaRepository.class);
    var adapter = new PageModelPersistenceAdapter(repository);
    var managed = managedEntity();

    when(repository.findById(PAGE_MODEL_UUID)).thenReturn(Optional.of(managed));
    when(repository.saveAll(List.of(managed))).thenReturn(List.of(managed));

    var saved = adapter.saveAll(List.of(instance()));

    assertThat(saved).hasSize(1);
    verify(repository).findById(PAGE_MODEL_UUID);
    verify(repository).saveAll(List.of(managed));
    assertThat(managed.getVersion()).isEqualTo(7L);
  }

  private static PageModelInstance instance() {
    return PageModelInstance.rehydrate(
        PAGE_MODEL_UUID,
        TENANT_UUID,
        "private.dashboard.tenant_admin",
        "private",
        "dashboard",
        PageModelStatus.DRAFT,
        3,
        JsonUtils.emptyObject(),
        null,
        Instant.parse("2026-07-31T10:00:00Z"),
        Instant.parse("2026-07-31T10:00:00Z"),
        ACTOR_UUID,
        ACTOR_UUID,
        null,
        null,
        null);
  }

  private static PageModelJpaEntity managedEntity() {
    var entity = new PageModelJpaEntity();
    entity.setId(PAGE_MODEL_UUID);
    entity.setTenantId(TENANT_UUID);
    entity.setLogicalId("private.dashboard.tenant_admin");
    entity.setScope("private");
    entity.setSlug("dashboard");
    entity.setStatus(PageModelStatus.DRAFT);
    entity.setSchemaVersion(3);
    entity.setModel(JsonUtils.emptyObject());
    entity.setSchema(JsonUtils.emptyObject());
    entity.setCode("private.dashboard.tenant_admin-3");
    entity.setName("private.dashboard.tenant_admin-3");
    entity.setCreatedAt(Instant.parse("2026-07-31T10:00:00Z"));
    entity.setUpdatedAt(Instant.parse("2026-07-31T10:00:00Z"));
    entity.setCreatedBy(ACTOR_UUID);
    entity.setUpdatedBy(ACTOR_UUID);
    entity.setVersion(7L);
    return entity;
  }
}
