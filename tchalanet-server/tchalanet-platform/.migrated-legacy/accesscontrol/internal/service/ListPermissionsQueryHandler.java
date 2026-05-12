package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListPermissionsRequestHandler
    implements QueryHandler<ListPermissionsRequest, List<PermissionView>> {

  private final PermissionCatalogAdminPort permissionCatalogAdminPort;

  @Override
  public List<PermissionView> handle(ListPermissionsRequest query) {
    return permissionCatalogAdminPort.listPermissions().stream()
        .map(
            summary ->
                new PermissionView(
                    summary.code(), summary.name(), summary.category(), summary.description()))
        .toList();
  }
}

