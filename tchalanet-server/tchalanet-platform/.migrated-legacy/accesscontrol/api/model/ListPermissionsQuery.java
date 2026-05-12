package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Query;
import java.util.List;

public record ListPermissionsRequest() implements Query<List<PermissionView>> {}

