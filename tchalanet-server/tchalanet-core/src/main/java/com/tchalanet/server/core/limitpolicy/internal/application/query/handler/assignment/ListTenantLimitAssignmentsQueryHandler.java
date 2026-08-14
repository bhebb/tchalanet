package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.assignment;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListTenantLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListTenantLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTenantLimitAssignmentsQueryHandler
    implements QueryHandler<ListTenantLimitAssignmentsQuery, ListTenantLimitAssignmentsView> {

  private final LimitAssignmentReaderPort reader;

  @Override
  public ListTenantLimitAssignmentsView handle(ListTenantLimitAssignmentsQuery query) {
    var items =
        reader.listByTenant(query.tenantId()).stream()
            .filter(a -> !a.deleted())
            .map(
                a ->
                    new ListTenantLimitAssignmentsView.Item(
                        a.id(),
                        a.ruleKey(),
                        toQueryRef(a.scope()),
                        a.enabled(),
                        a.onBreach(),
                        a.params(),
                        a.startsAt(),
                        a.endsAt()))
            .toList();

    return new ListTenantLimitAssignmentsView(items);
  }

  private LimitScopeQueryRef toQueryRef(LimitScopeRef scope) {
    return switch (scope) {
      case LimitScopeRef.TenantScope tenant -> LimitScopeQueryRef.tenant(tenant.tenantId());
      case LimitScopeRef.AgentScope agent -> LimitScopeQueryRef.agent(agent.userId());
      case LimitScopeRef.SellerTerminalScope terminal ->
          LimitScopeQueryRef.sellerTerminal(terminal.sellerTerminalId());
      case LimitScopeRef.DrawChannelScope channel ->
          LimitScopeQueryRef.drawChannel(channel.drawChannelId());
    };
  }
}
