package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates full tenant user provisioning:
 * 1. Create app_user + KC identity (with correct tenantCode in JWT claim).
 * 2. Create tenant membership.
 * 3. Assign the requested role via access-control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUserProvisioningService {

  private final UserAdminService userAdminService;
  private final TenantMembershipService tenantMembershipService;
  private final AccessControlApi accessControlApi;

  /**
   * Provisions a new user in a tenant with an explicit tenantCode.
   * The tenantCode is passed directly to KC provisioning so the JWT
   * tenant_code claim is correct on the very first login.
   */
  @Transactional
  public CreateUserResult provisionTenantUser(
      TenantId tenantId,
      String tenantCode,
      String email,
      String firstName,
      String lastName,
      TchRole role) {

    var created = userAdminService.createUserForTenant(email, null, firstName, lastName, tenantCode);

    tenantMembershipService.assign(tenantId, created.userId(), null, null, false);

    if (role != null) {
      accessControlApi.assignRoleToUser(
          new AssignRoleToUserRequest(tenantId, created.userId(), role.name(), null));
      log.info("Provisioned user {} in tenant {} with role {}", created.userId(), tenantId, role);
    }

    return created;
  }
}
