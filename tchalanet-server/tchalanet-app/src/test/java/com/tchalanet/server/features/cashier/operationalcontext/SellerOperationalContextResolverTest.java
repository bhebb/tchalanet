package com.tchalanet.server.features.cashier.operationalcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerOperationalContextResolverTest {

  @Test
  void resolvesSellerOperationalContextForSell() {
    var ids = ids();
    var resolver = resolver(true);

    var result = resolver.resolve(new ResolveSellerOperationalContextRequest(
        context(ids),
        ids.terminalId(),
        SellerOperation.SELL));

    assertEquals(ids.tenantId(), result.tenantId());
    assertEquals(ids.userId(), result.actorUserId());
    assertEquals(ids.terminalId(), result.terminalId());
    assertEquals(ids.outletId(), result.outletId());
    assertEquals(ids.sessionId(), result.salesSessionId());
  }

  @Test
  void rejectsWhenPermissionIsMissing() {
    var ids = ids();
    var resolver = resolver(false);

    assertThrows(ProblemRestException.class, () -> resolver.resolve(new ResolveSellerOperationalContextRequest(
        context(ids),
        ids.terminalId(),
        SellerOperation.SELL)));
  }

  @Test
  void mapsPrintReprintAndSendToTerminalValidationOperations() {
    var ids = ids();
    var queryBus = new CapturingQueryBus();
    var resolver = resolver(true, queryBus);

    resolver.resolve(new ResolveSellerOperationalContextRequest(
        context(ids),
        ids.terminalId(),
        SellerOperation.PRINT_TICKET));
    assertEquals(TerminalOperation.PRINT_TICKET, queryBus.lastTerminalOperation());

    resolver.resolve(new ResolveSellerOperationalContextRequest(
        context(ids),
        ids.terminalId(),
        SellerOperation.REPRINT_TICKET));
    assertEquals(TerminalOperation.REPRINT_TICKET, queryBus.lastTerminalOperation());

    resolver.resolve(new ResolveSellerOperationalContextRequest(
        context(ids),
        ids.terminalId(),
        SellerOperation.SEND_RECEIPT));
    assertEquals(TerminalOperation.PRINT_TICKET, queryBus.lastTerminalOperation());
  }

  private SellerOperationalContextResolver resolver(boolean permissionAllowed) {
    return resolver(permissionAllowed, new NoopQueryBus());
  }

  private SellerOperationalContextResolver resolver(boolean permissionAllowed, QueryBus queryBus) {
    return new SellerOperationalContextResolver(
        queryBus,
        new NoopIdentityApi(),
        new TestAccessControlApi(permissionAllowed));
  }

  private TchRequestContext context(Ids ids) {
    return new TchRequestContext(
        "demo",
        ids.tenantId().value(),
        "demo",
        ids.tenantId().value(),
        "keycloak-user",
        ids.userId().value(),
        EnumSet.of(TchRole.CASHIER),
        Set.of(),
        Locale.CANADA_FRENCH,
        "req-1",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        ids.tenantId(),
        ZoneId.of("America/Toronto"),
        Currency.getInstance("CAD"),
        new OperationalContextHint(
            ids.terminalId(),
            ids.outletId(),
            ids.sessionId(),
            OperationalContextSource.ADMIN_SELECTION,
            OperationalContextTrust.WEAK));
  }

  private Ids ids() {
    return new Ids(
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000004")),
        SalesSessionId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")));
  }

  private record Ids(
      TenantId tenantId,
      UserId userId,
      TerminalId terminalId,
      OutletId outletId,
      SalesSessionId sessionId) {}

  private static class NoopQueryBus implements QueryBus {
    @Override
    public <R> R ask(Query<R> query) {
      return null;
    }
  }

  private static class CapturingQueryBus implements QueryBus {
    private TerminalOperation lastTerminalOperation;

    @Override
    public <R> R ask(Query<R> query) {
      if (query instanceof ValidateTerminalForOperationQuery terminalQuery) {
        lastTerminalOperation = terminalQuery.operation();
      }
      return null;
    }

    TerminalOperation lastTerminalOperation() {
      return lastTerminalOperation;
    }
  }

  private static class NoopIdentityApi implements IdentityApi {
    @Override
    public CurrentUserView getCurrentUser(GetCurrentUserRequest request) {
      return null;
    }

    @Override
    public BootstrapUserResult bootstrapCurrentUser(BootstrapCurrentUserRequest request) {
      return null;
    }

    @Override
    public UserProfileView getUserProfile(GetUserProfileRequest request) {
      return null;
    }

    @Override
    public Optional<AppUserView> findAppUser(UUID userId) {
      return Optional.empty();
    }

    @Override
    public long countTenantUsers() {
      return 0;
    }
  }

  private record TestAccessControlApi(boolean permissionAllowed) implements AccessControlApi {
    @Override
    public CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request) {
      return new CheckUserPermissionsResult(
          permissionAllowed,
          permissionAllowed ? Set.of() : Set.of("ticket.sell"));
    }

    @Override
    public List<RoleView> listRoles(ListRolesRequest request) {
      return List.of();
    }

    @Override
    public List<PermissionView> listPermissions(ListPermissionsRequest request) {
      return List.of();
    }

    @Override
    public List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request) {
      return List.of();
    }

    @Override
    public EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request) {
      return null;
    }

    @Override
    public RoleView createRole(CreateRoleRequest request) {
      return null;
    }

    @Override
    public RoleView updateRole(UpdateRoleRequest request) {
      return null;
    }

    @Override
    public void grantPermission(GrantPermissionToRoleRequest request) {
    }

    @Override
    public void revokePermission(RevokePermissionFromRoleRequest request) {
    }

    @Override
    public void setTenantUserRole(SetTenantUserRoleRequest request) {
    }
  }
}
