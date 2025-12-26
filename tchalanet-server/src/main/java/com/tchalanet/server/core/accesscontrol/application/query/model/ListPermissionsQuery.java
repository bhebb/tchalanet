package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort.PermissionSummary;
import java.util.List;

public record ListPermissionsQuery() implements Query<List<PermissionSummary>> {}
