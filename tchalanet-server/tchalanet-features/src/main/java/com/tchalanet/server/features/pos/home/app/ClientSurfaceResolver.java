package com.tchalanet.server.features.pos.home.app;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurfacePolicy;
import org.springframework.stereotype.Component;

@Component
public class ClientSurfaceResolver {

  public static final String HEADER_NAME = "X-Tch-Surface";

  public ClientSurface resolve(TchRequestContext ctx, String requestedSurface) {
    var available = ClientSurfacePolicy.availableSurfaces(ctx.systemRoles());
    var surface = requestedSurface == null || requestedSurface.isBlank()
        ? ClientSurfacePolicy.preferredSurface(ctx.systemRoles())
        : parse(requestedSurface);
    if (!available.contains(surface)) {
      throw ProblemRest.forbidden("surface.not_allowed");
    }
    return surface;
  }

  private ClientSurface parse(String requestedSurface) {
    try {
      return ClientSurface.valueOf(requestedSurface.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.forbidden("surface.not_allowed");
    }
  }
}
