package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListRolesRequestHandler implements QueryHandler<ListRolesRequest, List<RoleView>> {

  private final RoleReaderPort roleReaderPort;

  @Override
  public List<RoleView> handle(ListRolesRequest query) {
    TenantId tenantId = query.tenantId();

    // Delegate reading to the outgoing port so adapters handle entity <-> domain mapping
    return roleReaderPort.listSystemRolesForTenant(tenantId).stream()
        .map(role -> new RoleView(null, role.name(), role.name(), null, null, null, true))
        .toList();
  }
}

