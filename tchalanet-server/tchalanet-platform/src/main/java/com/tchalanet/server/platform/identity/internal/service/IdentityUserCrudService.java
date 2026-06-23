package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unified CRUD actions on any platform user (SUPER_ADMIN or TENANT_ADMIN).
 * Hierarchy rule: a TENANT_ADMIN caller cannot act on a user who holds a platform-scope role
 * (i.e. SUPER_ADMIN). A SUPER_ADMIN can act on anyone.
 */
@Service
@RequiredArgsConstructor
public class IdentityUserCrudService {

  private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

  private final TenantUserAdministrationService users;
  private final AppUserJpaAdapter userAdapter;
  private final PlatformUserRoleJpaRepository platformUserRoles;
  private final TenantMembershipService tenantMemberships;
  private final AccessControlApi accessControl;
  private final IdentityProvisioningApi identityProvisioning;
  private final TemporaryCredentialService credentials;
  private final CommunicationApi communication;

  @Transactional
  public void activate(UserId targetId, TchRequestContext ctx) {
    assertCanActOn(targetId, ctx);
    users.reactivateUser(targetId);
  }

  @Transactional
  public void suspend(UserId targetId, TchRequestContext ctx) {
    assertCanActOn(targetId, ctx);
    users.suspendUser(targetId);
  }

  @Transactional
  public void archive(UserId targetId, TchRequestContext ctx) {
    assertCanActOn(targetId, ctx);
    users.deleteUser(targetId);
  }

  @Transactional
  public String resetPassword(UserId targetId, TchRequestContext ctx) {
    assertCanActOn(targetId, ctx);

    var target = userAdapter.findById(targetId)
        .orElseThrow(() -> ProblemRest.notFound("User not found"));
    var externalSubject = userAdapter.findExternalSubject(targetId, IdentityProviderType.FIREBASE)
        .orElseThrow(() -> ProblemRest.unprocessable("No Firebase identity linked for this account"));

    var tempPassword = credentials.adminTemporaryPassword();
    identityProvisioning.resetPassword(externalSubject, tempPassword);

    var meta = Map.<String, Object>of("tempPassword", tempPassword);
    if (target.email() != null && !target.email().isBlank()) {
      communication.sendNow(new SendOutboundMessageRequest(
          "identity.credential.reset", CommunicationChannel.EMAIL,
          OutboundRecipient.of(target.email()), Locale.FRENCH, meta));
    }
    if (target.phone() != null && !target.phone().isBlank()) {
      communication.sendNow(new SendOutboundMessageRequest(
          "identity.credential.reset", CommunicationChannel.SMS,
          OutboundRecipient.of(target.phone()), Locale.FRENCH, meta));
    }
    return tempPassword;
  }

  public List<AppUser> searchUnassigned(String q, int page, int size) {
    return userAdapter.findUnassigned(q, page, size);
  }

  @Transactional
  public UserId createUser(String email, String phone, String firstName, String lastName) {
    var result = users.createUser(email, phone, firstName, lastName, null, null, null, null, null, false, Set.of());
    return result.userId();
  }

  @Transactional
  public void assignMembership(UserId targetId, TenantId tenantId, TchRole role, TchRequestContext ctx) {
    boolean callerIsSuperAdmin = ctx.roleCodes() != null && ctx.roleCodes().contains(SUPER_ADMIN_ROLE);
    if (!callerIsSuperAdmin) {
      var callerTenant = ctx.tenantId();
      if (callerTenant == null || !callerTenant.equals(tenantId)) {
        throw ProblemRest.forbidden("Cannot assign membership to a different tenant");
      }
    }
    tenantMemberships.assign(tenantId, targetId, false);
    accessControl.assignRoleToUser(
        new AssignRoleToUserRequest(tenantId, targetId, role.name(), ctx.userId()));
  }

  // A TENANT_ADMIN may not act on users who hold a platform-scope (SUPER_ADMIN) role.
  private void assertCanActOn(UserId targetId, TchRequestContext ctx) {
    boolean callerIsSuperAdmin = ctx.roleCodes() != null && ctx.roleCodes().contains(SUPER_ADMIN_ROLE);
    if (callerIsSuperAdmin) {
      return;
    }
    boolean targetIsPlatformUser = !platformUserRoles.findPlatformRoleAccessRows(targetId.value()).isEmpty();
    if (targetIsPlatformUser) {
      throw ProblemRest.forbidden("Insufficient authority to act on this user");
    }
  }
}
