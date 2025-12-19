package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionAdminPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolePermissionsQuery;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListRolePermissionsQueryHandler
    implements QueryHandler<ListRolePermissionsQuery, Set<String>> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public Set<String> handle(ListRolePermissionsQuery query) {
    return rolePermissionAdminPort.listPermissionCodes(query.roleId());
  }
}

