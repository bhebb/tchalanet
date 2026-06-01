package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Read access to the permission catalog (what the backend can protect). */
@Service
@RequiredArgsConstructor
public class PermissionRegistryService {

  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;

  public List<PermissionView> listPermissions(ListPermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissions().stream()
        .map(s -> new PermissionView(s.code(), s.name(), s.category(), s.description()))
        .toList();
  }
}
