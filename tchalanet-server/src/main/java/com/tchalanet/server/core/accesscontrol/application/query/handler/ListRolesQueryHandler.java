package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.accesscontrol.application.port.out.RoleReaderPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolesQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListRolesQueryHandler implements QueryHandler<ListRolesQuery, List<TchRole>> {

  private final RoleReaderPort roleReaderPort;

  @Override
  public List<TchRole> handle(ListRolesQuery query) {
    TenantId tenantId = query.tenantId();

    // Delegate reading to the outgoing port so adapters handle entity <-> domain mapping
    return roleReaderPort.listSystemRolesForTenant(tenantId);
  }
}
