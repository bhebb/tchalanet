package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates full tenant user provisioning:
 * 1. Create Firebase identity + app_user + durable external identity mapping.
 * 2. Create tenant membership.
 * 3. Assign the requested role via access-control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUserProvisioningService {

  private final TenantUserAdministrationService userAdminService;
  private final TenantMembershipService tenantMembershipService;
  private final AccessControlApi accessControlApi;

  /**
   * Provisions a new user in a tenant with an explicit tenantCode.
   * Authorization and tenant context are resolved from Tchalanet after authentication.
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
    assignMembershipAndRole(tenantId, created.userId(), role, null);
    return created;
  }

  /**
   * Provisions a new user from the admin controller, in the current request's tenant context.
   * The membership carries the requested outlet/terminal, and {@code actor} is recorded as the
   * role assigner.
   */
  @Transactional
  public CreateUserResult provisionTenantUser(
      TenantId tenantId,
      UserId actor,
      String email,
      String phone,
      String firstName,
      String lastName,
      TchRole role) {

    var created =
        userAdminService.createUser(
            email, phone, firstName, lastName, null, null, null, null, null, false, Set.of());
    assignMembershipAndRole(tenantId, created.userId(), role, actor);
    return created;
  }

  private void assignMembershipAndRole(
      TenantId tenantId, UserId userId, TchRole role, UserId actor) {
    tenantMembershipService.assign(tenantId, userId, false);
    if (role != null) {
      accessControlApi.assignRoleToUser(new AssignRoleToUserRequest(tenantId, userId, role.name(), actor));
      log.info("Provisioned user {} in tenant {} with role {}", userId, tenantId, role);
    }
  }
}
