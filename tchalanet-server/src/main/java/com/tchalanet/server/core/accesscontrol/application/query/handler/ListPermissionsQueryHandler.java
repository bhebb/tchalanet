package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort.PermissionSummary;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListPermissionsQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListPermissionsQueryHandler
    implements QueryHandler<ListPermissionsQuery, List<PermissionSummary>> {

  private final PermissionCatalogAdminPort permissionCatalogAdminPort;

  @Override
  public List<PermissionSummary> handle(ListPermissionsQuery query) {
    return permissionCatalogAdminPort.listPermissions();
  }
}

