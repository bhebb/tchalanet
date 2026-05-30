package com.tchalanet.server.core.outlet.internal.infra.web.tenant.model;

import com.tchalanet.server.core.outlet.api.command.lifecycle.CloseDayMode;
import jakarta.validation.constraints.Size;

/**
 * Seller-facing "close my current outlet for today" body. The outlet and tenant
 * come from the trusted operational context — never the client. {@code mode}
 * defaults to STRICT (refuses if open sessions exist).
 */
public record CloseCurrentOutletDayRequest(
    CloseDayMode mode,
    @Size(max = 255) String reason) {

  public CloseDayMode modeOrDefault() {
    return mode == null ? CloseDayMode.STRICT : mode;
  }
}
