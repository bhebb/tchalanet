package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("TenantUserProvisioningService")
class TenantUserProvisioningServiceTest {

  private final TenantUserAdministrationService userAdminService = mock(TenantUserAdministrationService.class);
  private final TenantMembershipService memberships = mock(TenantMembershipService.class);
  private final AccessControlApi accessControlApi = mock(AccessControlApi.class);

  private final TenantUserProvisioningService service =
      new TenantUserProvisioningService(userAdminService, memberships, accessControlApi);

  @Test
  @DisplayName("admin-context provisioning creates user, membership and app-owned role assignment")
  void provisionsUserMembershipAndRole() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var actor = UserId.of(UUID.randomUUID());
    var createdUserId = UserId.of(UUID.randomUUID());
    when(userAdminService.createUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(new CreateUserResult(createdUserId));

    var result = service.provisionTenantUser(
        tenantId, actor, "cashier@tchalanet.test", "+509", "Cash", "Ier", TchRole.CASHIER);

    assertThat(result.userId()).isEqualTo(createdUserId);
    verify(memberships).assign(tenantId, createdUserId, false);

    var roleReq = ArgumentCaptor.forClass(AssignRoleToUserRequest.class);
    verify(accessControlApi).assignRoleToUser(roleReq.capture());
    assertThat(roleReq.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(roleReq.getValue().userId()).isEqualTo(createdUserId);
    assertThat(roleReq.getValue().roleCode()).isEqualTo("CASHIER");
    assertThat(roleReq.getValue().assignedBy()).isEqualTo(actor);

  }

  @Test
  @DisplayName("membership is assigned without touching access-control when no role is requested")
  void membershipAssignmentDoesNotTouchAccessControl() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var actor = UserId.of(UUID.randomUUID());
    var createdUserId = UserId.of(UUID.randomUUID());
    when(userAdminService.createUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(new CreateUserResult(createdUserId));

    service.provisionTenantUser(
        tenantId, actor, "member@tchalanet.test", null, "Mem", "Ber", null);

    verify(memberships).assign(tenantId, createdUserId, false);
    verifyNoInteractions(accessControlApi);
  }

  @Test
  @DisplayName("onboarding path uses explicit tenant code, no outlet/terminal, and assigns the role")
  void onboardingPathUsesExplicitTenantCode() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var createdUserId = UserId.of(UUID.randomUUID());
    when(userAdminService.createUserForTenant(any(), isNull(), any(), any(), eq("acme")))
        .thenReturn(new CreateUserResult(createdUserId));

    service.provisionTenantUser(tenantId, "acme", "admin@tchalanet.test", "Ada", "Min", TchRole.TENANT_ADMIN);

    verify(memberships).assign(tenantId, createdUserId, false);
    verify(accessControlApi).assignRoleToUser(any(AssignRoleToUserRequest.class));
  }
}
