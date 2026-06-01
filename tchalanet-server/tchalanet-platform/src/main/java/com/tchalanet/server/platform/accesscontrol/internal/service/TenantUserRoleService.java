package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveRoleFromUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.TenantUserRoleJpaAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assigns and removes system roles for a tenant user (operates on {@code tenant_user_role}). */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUserRoleService {

  private final TenantUserRoleJpaAdapter tenantUserRoleAdapter;
  private final TenantUserRoleWriterPort tenantUserRoleWriter;
  private final AppRoleJpaRepository appRoleRepository;

  @Transactional
  public void assignRoleToUser(AssignRoleToUserRequest request) {
    tenantUserRoleAdapter.assign(request.tenantId(), request.userId(), request.roleCode(), request.assignedBy());
    log.info("Assigned role {} to user {} in tenant {}", request.roleCode(), request.userId(), request.tenantId());
  }

  @Transactional
  public void removeRoleFromUser(RemoveRoleFromUserRequest request) {
    tenantUserRoleAdapter.remove(request.tenantId(), request.userId(), request.roleCode());
    log.info("Removed role {} from user {} in tenant {}", request.roleCode(), request.userId(), request.tenantId());
  }

  /** @deprecated use {@link #assignRoleToUser(AssignRoleToUserRequest)} instead */
  @Deprecated
  @Transactional
  public void setTenantUserRole(SetTenantUserRoleRequest request) {
    var roleEntity = appRoleRepository.findByCode(request.role().name())
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));
    tenantUserRoleWriter.setUserRole(request.tenantId(), request.userId(), RoleId.of(roleEntity.getId()));
  }
}
