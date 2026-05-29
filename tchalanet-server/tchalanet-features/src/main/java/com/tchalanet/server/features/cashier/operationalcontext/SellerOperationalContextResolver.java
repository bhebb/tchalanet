package com.tchalanet.server.features.cashier.operationalcontext;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SellerOperationalContextResolver {

  private static final Set<String> SELL_PERMISSIONS = Set.of("TICKET_SELL");

  private final QueryBus queryBus;
  private final IdentityApi identityApi;
  private final AccessControlApi accessControlApi;

  public SellerOperationalContextResolver(
      QueryBus queryBus,
      IdentityApi identityApi,
      AccessControlApi accessControlApi) {
    this.queryBus = queryBus;
    this.identityApi = identityApi;
    this.accessControlApi = accessControlApi;
  }

  public SellerOperationalContextView resolve(ResolveSellerOperationalContextRequest request) {
    var ctx = request.requestContext();
    var tenantId = ctx.effectiveTenantIdRequired();
    var actorUserId = ctx.currentUserIdRequired();
    var operationalContext = Objects.requireNonNull(ctx.operationalContext(), "seller operational context is required");
    var terminalId = request.terminalId();
    var outletId = operationalContext.outletId();
    var salesSessionId = operationalContext.salesSessionId();

    if (!operationalContext.terminalId().equals(terminalId)) {
      throw ProblemRest.forbidden("seller_context.terminal_mismatch");
    }

    identityApi.getUserProfile(new GetUserProfileRequest(actorUserId));
    //assertAllowed(tenantId, actorUserId, permissionsFor(request.operation()));

    queryBus.ask(new ValidateTerminalForOperationQuery(
        tenantId,
        terminalId,
        outletId,
        actorUserId,
        terminalOperation(request.operation())));

    queryBus.ask(new ValidateOutletForOperationQuery(
        tenantId,
        outletId,
        outletOperation(request.operation())));

    queryBus.ask(new ValidateSalesSessionForOperationQuery(
        tenantId,
        salesSessionId,
        terminalId,
        outletId,
        actorUserId,
        sessionOperation(request.operation())));

    return new SellerOperationalContextView(
        tenantId,
        actorUserId,
        terminalId,
        outletId,
        salesSessionId,
        ctx.locale(),
        ctx.tenantZoneId(),
        ctx.tenantCurrency(),
        permissionsFor(request.operation()));
  }

  private void assertAllowed(
      com.tchalanet.server.common.types.id.TenantId tenantId,
      com.tchalanet.server.common.types.id.UserId userId,
      Set<String> permissions) {
    var result = accessControlApi.checkPermissions(
        new CheckUserPermissionsRequest(tenantId, userId, permissions));
    if (!result.allowed()) {
      throw ProblemRest.forbidden("seller_context.permission_denied");
    }
  }

  private Set<String> permissionsFor(SellerOperation operation) {
    return switch (operation) {
      case SELL, PRINT_TICKET, REPRINT_TICKET, SEND_RECEIPT -> SELL_PERMISSIONS;
    };
  }

  private TerminalOperation terminalOperation(SellerOperation operation) {
    return switch (operation) {
      case SELL -> TerminalOperation.SELL_TICKET;
      case PRINT_TICKET, SEND_RECEIPT -> TerminalOperation.PRINT_TICKET;
      case REPRINT_TICKET -> TerminalOperation.REPRINT_TICKET;
    };
  }

  private OutletOperation outletOperation(SellerOperation operation) {
    return switch (operation) {
      case SELL, PRINT_TICKET, REPRINT_TICKET, SEND_RECEIPT -> OutletOperation.SELL;
    };
  }

  private SalesSessionOperation sessionOperation(SellerOperation operation) {
    return switch (operation) {
      case SELL, PRINT_TICKET, REPRINT_TICKET, SEND_RECEIPT -> SalesSessionOperation.SELL;
    };
  }
}
