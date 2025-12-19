package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.List;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort.PermissionSummary;

public record ListPermissionsQuery() implements Query<List<PermissionSummary>> {}

