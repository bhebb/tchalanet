package com.tchalanet.server.platform.ops.api;

import java.util.List;

/**
 * Contributes operational resource status lines to the platform Ops dashboard.
 *
 * <p>Implementations may live in the app layer when they depend on runtime,
 * cache, actuator, or identity-provider infrastructure.
 */
public interface OpsResourceContributor {

  List<OpsServiceResourceItem> services();
}
