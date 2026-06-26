package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import java.util.List;

/**
 * Port: contributes operational resource status lines to the platform Ops dashboard.
 *
 * <p>Implementations live in the composition/app layer when they depend on runtime,
 * cache, actuator, or identity-provider infrastructure.
 */
public interface OpsResourceContributor {

  List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services();
}
