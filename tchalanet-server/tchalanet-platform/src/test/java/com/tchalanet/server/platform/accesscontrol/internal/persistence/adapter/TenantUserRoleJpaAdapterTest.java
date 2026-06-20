package com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.TenantUserRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TenantUserRoleJpaAdapterTest {

  private final TenantUserRoleJpaRepository tenantUserRoleRepository = mock(TenantUserRoleJpaRepository.class);
  private final AppRoleJpaRepository appRoleRepository = mock(AppRoleJpaRepository.class);
  private final TenantUserRoleJpaAdapter adapter =
      new TenantUserRoleJpaAdapter(tenantUserRoleRepository, appRoleRepository);

  @Test
  void assignResolvesTenantScopedSystemRole() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var assignedBy = UserId.of(UUID.randomUUID());
    var tenantRoleId = UUID.randomUUID();
    var role = new AppRoleJpaEntity();
    role.setId(tenantRoleId);
    role.setCode("TENANT_ADMIN");
    role.setScope("TENANT");

    when(appRoleRepository.findActiveSystemRoleByCodeAndScope("TENANT_ADMIN", "TENANT"))
        .thenReturn(Optional.of(role));
    when(tenantUserRoleRepository.findActiveAssignment(tenantId.value(), userId.value(), tenantRoleId))
        .thenReturn(Optional.empty());

    adapter.assign(tenantId, userId, "TENANT_ADMIN", assignedBy);

    var saved = ArgumentCaptor.forClass(TenantUserRoleJpaEntity.class);
    verify(tenantUserRoleRepository).save(saved.capture());
    assertThat(saved.getValue().getRoleId()).isEqualTo(tenantRoleId);
    assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId.value());
    assertThat(saved.getValue().getUserId()).isEqualTo(userId.value());
    verify(appRoleRepository, never()).findByCode("TENANT_ADMIN");
  }

  @Test
  void removeResolvesTenantScopedSystemRole() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var tenantRoleId = UUID.randomUUID();
    var role = new AppRoleJpaEntity();
    role.setId(tenantRoleId);
    role.setCode("TENANT_ADMIN");
    role.setScope("TENANT");

    when(appRoleRepository.findActiveSystemRoleByCodeAndScope("TENANT_ADMIN", "TENANT"))
        .thenReturn(Optional.of(role));

    adapter.remove(tenantId, userId, "TENANT_ADMIN");

    verify(tenantUserRoleRepository).softDeleteAssignment(tenantId.value(), userId.value(), tenantRoleId);
    verify(appRoleRepository, never()).findByCode("TENANT_ADMIN");
  }
}
